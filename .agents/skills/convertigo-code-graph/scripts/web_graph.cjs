// Parse code, never import or execute application modules.
const fs = require("node:fs");
const path = require("node:path");
const { createRequire } = require("node:module");

function extract(root, dependencyRoot, names) {
  const requireWeb = createRequire(
    path.join(dependencyRoot, "convertigo-studio-web/package.json"),
  );
  const babel = requireWeb("@babel/parser");
  const traverse = requireWeb("@babel/traverse").default;
  const types = requireWeb("@babel/types");
  const svelte = requireWeb("svelte/compiler");
  const nodes = [],
    edges = [],
    requests = [],
    warnings = [],
    modules = new Map();
  const localCalls = [],
    dynamicCalls = [];
  const serviceFile = "convertigo-studio-web/src/lib/utils/service.js";
  const helperFile =
    "convertigo-studio-web/src/lib/common/ServiceHelper.svelte.js";
  const suffixes = [
    "",
    ".js",
    ".ts",
    ".svelte.js",
    ".svelte.ts",
    ".svelte",
    "/index.js",
    "/index.ts",
  ];
  const selected = new Set(names);
  const fileId = (name) => `web:${name}`;
  const resolve = (name, specifier) => {
    let candidate;
    if (specifier.startsWith("$lib/"))
      candidate = `convertigo-studio-web/src/lib/${specifier.slice(5)}`;
    else if (specifier.startsWith("."))
      candidate = path.posix.normalize(
        path.posix.join(path.posix.dirname(name), specifier),
      );
    else return null;
    return (
      suffixes
        .map((suffix) => candidate + suffix)
        .find((p) => selected.has(p)) ?? null
    );
  };
  const addNode = (id, label, entity_type, name, line) => {
    nodes.push({
      id,
      label,
      entity_type,
      language: name.endsWith(".svelte") ? "svelte" : "javascript/typescript",
      file_type: "code",
      source_file: name,
      source_location: `L${line}`,
      confidence: "EXTRACTED",
      _origin: "ast",
    });
    return id;
  };
  const addEdge = (
    source,
    target,
    relation,
    name,
    line,
    confidence = "EXTRACTED",
  ) => {
    if (source && target)
      edges.push({
        source,
        target,
        relation,
        source_file: name,
        source_location: `L${line}`,
        confidence,
        weight: 1,
        _origin: "ast",
      });
  };
  const options = {
    sourceType: "unambiguous",
    plugins: ["typescript", "jsx"],
    errorRecovery: false,
    allowAwaitOutsideFunction: true,
  };

  // First pass: declarations and lexical scopes, before resolving cross-file exports.
  for (const name of names) {
    const source = fs.readFileSync(path.join(root, name), "utf8");
    if (name.endsWith(".json")) {
      addNode(
        fileId(name),
        path.posix.basename(name),
        "configuration",
        name,
        1,
      );
      continue;
    }
    const id = addNode(
      fileId(name),
      path.posix.basename(name),
      name.endsWith(".svelte") ? "component" : "module",
      name,
      1,
    );
    const mod = {
      name,
      source,
      id,
      trees: [],
      functions: new Map(),
      functionScopes: new Map(),
      exports: new Map(),
      imports: [],
      programScopes: [],
    };
    modules.set(name, mod);
    try {
      if (name.endsWith(".svelte")) {
        const parsed = svelte.parse(source, { modern: true, filename: name });
        mod.fragment = parsed.fragment;
        for (const script of [parsed.module, parsed.instance].filter(Boolean)) {
          const start = script.content.start,
            end = script.content.end;
          // Padding retains original offsets and lines for Babel's scoped traversal.
          const padded =
            source.slice(0, start).replace(/[^\r\n]/g, " ") +
            source.slice(start, end);
          mod.trees.push(babel.parse(padded, options));
        }
      } else mod.trees.push(babel.parse(source, options));
      for (const tree of mod.trees) {
        traverse(tree, {
          Program(p) {
            mod.programScopes.push(p.scope);
          },
          Function(p) {
            const n = p.node;
            const label =
              n.id?.name ??
              (p.parentPath.isVariableDeclarator() ? p.parent.id.name : null) ??
              n.key?.name ??
              n.key?.value ??
              `callback@${n.loc.start.line}`;
            const fid = addNode(
              `${id}@${n.start}`,
              label,
              "function",
              name,
              n.loc.start.line,
            );
            mod.functions.set(n, fid);
            mod.functionScopes.set(n, p.scope);
            const outer = p.findParent((q) => q.isFunction());
            addEdge(
              outer ? mod.functions.get(outer.node) : id,
              fid,
              "contains",
              name,
              n.loc.start.line,
            );
          },
          ImportDeclaration(p) {
            if (p.node.importKind === "type") return;
            const target = resolve(name, p.node.source.value);
            mod.imports.push({
              target,
              specifier: p.node.source.value,
              line: p.node.loc.start.line,
            });
            if (target)
              addEdge(
                id,
                fileId(target),
                "imports",
                name,
                p.node.loc.start.line,
              );
            else if (
              (p.node.source.value.startsWith(".") ||
                p.node.source.value.startsWith("$lib/")) &&
              !p.node.source.value.endsWith(".css")
            ) {
              warnings.push({
                source_file: name,
                line: p.node.loc.start.line,
                reason: "unresolved local import",
                expression: p.node.source.value,
              });
            }
          },
        });
      }
    } catch (error) {
      warnings.push({
        source_file: name,
        reason: "parse error",
        line: error.loc?.line ?? error.start?.line ?? null,
        message: error.message,
      });
    }
  }

  function bindingTarget(mod, scope, name, seen = new Set()) {
    const binding = scope?.getBinding(name);
    if (!binding || !binding.constant || seen.has(binding)) return null;
    seen.add(binding);
    const p = binding.path;
    if (
      p.isImportSpecifier() ||
      p.isImportDefaultSpecifier() ||
      p.isImportNamespaceSpecifier()
    ) {
      const target = resolve(mod.name, p.parent.source.value);
      const exported = p.isImportDefaultSpecifier()
        ? "default"
        : (p.node.imported?.name ?? p.node.imported?.value);
      return {
        module: target,
        exported,
        id:
          exported === "default" && target?.endsWith(".svelte")
            ? fileId(target)
            : modules.get(target)?.exports.get(exported),
        kind: "import",
      };
    }
    if (p.isFunctionDeclaration())
      return {
        id: mod.functions.get(p.node),
        module: mod.name,
        exported: name,
      };
    if (p.isVariableDeclarator()) {
      let init = p.node.init;
      if (p.node.id.type === "ObjectPattern") {
        const property = p.node.id.properties.find(
          (property) =>
            property.value?.name === name ||
            property.value?.left?.name === name,
        );
        if (
          init?.type === "CallExpression" &&
          init.callee.name === "$derived" &&
          !p.scope.getBinding("$derived")
        )
          init = init.arguments[0];
        if (property && !property.computed && init?.type === "Identifier") {
          const imported = bindingTarget(mod, p.scope, init.name, seen);
          if (imported?.kind === "import") {
            const member = imported.exported
              ? `${imported.exported}.${property.key.name ?? property.key.value}`
              : (property.key.name ?? property.key.value);
            return {
              module: imported.module,
              exported: member,
              id: modules.get(imported.module)?.exports.get(member),
              candidate: true,
            };
          }
        }
        return null;
      }
      if (mod.functions.has(init))
        return {
          id: mod.functions.get(init),
          module: mod.name,
          exported: name,
        };
      if (init?.type === "Identifier")
        return bindingTarget(mod, p.scope, init.name, seen);
    }
    if (p.isAssignmentPattern() && p.node.right.type === "Identifier") {
      const result = bindingTarget(
        mod,
        p.scope.parent,
        p.node.right.name,
        seen,
      );
      if (result) return { ...result, conditional: true };
    }
    return null;
  }
  // Babel retains binding paths for parameters; defaults are candidates, not unconditional calls.
  function targetOf(mod, scope, expression) {
    if (expression?.type === "Identifier")
      return bindingTarget(mod, scope, expression.name);
    if (
      expression?.type === "MemberExpression" &&
      expression.object.type === "Identifier" &&
      !expression.computed
    ) {
      const imported = bindingTarget(mod, scope, expression.object.name);
      if (imported?.kind === "import") {
        const member = imported.exported
          ? `${imported.exported}.${expression.property.name}`
          : expression.property.name;
        return {
          module: imported.module,
          exported: member,
          id: modules.get(imported.module)?.exports.get(member),
          candidate: Boolean(imported.exported),
        };
      }
    }
    return null;
  }
  for (const mod of modules.values()) {
    for (const tree of mod.trees)
      traverse(tree, {
        ExportNamedDeclaration(p) {
          const declaration = p.node.declaration;
          if (declaration?.id?.name)
            mod.exports.set(
              declaration.id.name,
              mod.functions.get(declaration),
            );
          for (const d of declaration?.declarations ?? []) {
            if (d.id.type === "Identifier" && mod.functions.has(d.init))
              mod.exports.set(d.id.name, mod.functions.get(d.init));
          }
          if (!p.node.source)
            for (const s of p.node.specifiers) {
              const target = bindingTarget(mod, p.scope, s.local.name);
              if (target?.id)
                mod.exports.set(s.exported.name ?? s.exported.value, target.id);
            }
        },
        ExportDefaultDeclaration(p) {
          const n = p.node.declaration;
          const target =
            mod.functions.get(n) ??
            (n.type === "Identifier"
              ? bindingTarget(mod, p.scope, n.name)?.id
              : null);
          if (target) mod.exports.set("default", target);
          let object = n;
          if (
            n.type === "CallExpression" &&
            targetOf(mod, p.scope, n.callee)?.module === helperFile
          ) {
            mod.defaultConfigured = true;
            object = n.arguments[0]?.properties?.find(
              (property) =>
                (property.key?.name ?? property.key?.value) === "values",
            )?.value;
          }
          if (object?.type === "Identifier") {
            const binding = p.scope.getBinding(object.name);
            object =
              binding?.constant && binding.path.isVariableDeclarator()
                ? binding.path.node.init
                : null;
          }
          if (object?.type === "ObjectExpression")
            for (const property of object.properties) {
              const name = property.key?.name ?? property.key?.value;
              const id =
                mod.functions.get(property) ??
                mod.functions.get(property.value);
              if (name && id && !property.computed)
                mod.exports.set(`default.${name}`, id);
            }
        },
      });
  }

  let verifiedCall = false,
    verifiedPrefix = false,
    verifiedHelper = false;
  const protocolModule = modules.get(serviceFile);
  for (const tree of protocolModule?.trees ?? [])
    traverse(tree, {
      FunctionDeclaration(p) {
        if (p.node.id?.name === "getUrl") {
          const parameter = p.node.params[0];
          verifiedPrefix =
            parameter?.type === "AssignmentPattern" &&
            parameter.right.value === "admin/services/";
        }
        if (p.node.id?.name === "call") {
          let concatenation = false,
            fetch = false;
          p.traverse({
            BinaryExpression(q) {
              const n = q.node;
              if (
                n.operator === "+" &&
                n.left.type === "CallExpression" &&
                n.left.callee.name === "getUrl" &&
                !n.left.arguments.length &&
                n.right.name === "service"
              )
                concatenation = true;
            },
            CallExpression(q) {
              if (
                q.node.callee.name === "fetch" &&
                q.node.arguments[0]?.name === "url"
              )
                fetch = true;
            },
          });
          verifiedCall = concatenation && fetch;
        }
      },
    });
  for (const tree of modules.get(helperFile)?.trees ?? [])
    traverse(tree, {
      CallExpression(p) {
        if (
          p.node.callee.name === "call" &&
          p.node.arguments[0]?.name === "service"
        )
          verifiedHelper = true;
      },
    });
  if (!verifiedCall || !verifiedPrefix)
    warnings.push({
      source_file: serviceFile,
      reason: "HTTP prefix/call rule not verified; affected requests omitted",
    });

  if (verifiedCall && verifiedPrefix) {
    const transport = nodes.find(
      (n) => n.id === protocolModule.exports.get("call"),
    );
    if (transport) transport.transport_boundary = true;
  }

  // Only closed expressions produce endpoint names. Mutable bindings and parameters stay unresolved.
  function strings(scope, n, seen = new Set(), substitutions = new Map()) {
    if (!n) return null;
    if (
      n.type === "StringLiteral" ||
      (n.type === "Literal" && typeof n.value === "string")
    )
      return [n.value];
    if (n.type === "ConditionalExpression") {
      const a = strings(scope, n.consequent, seen, substitutions),
        b = strings(scope, n.alternate, seen, substitutions);
      return a && b ? [...new Set([...a, ...b])] : null;
    }
    if (n.type === "Identifier") {
      const binding = scope?.getBinding(n.name);
      if (binding?.constant && substitutions.has(binding))
        return substitutions.get(binding);
      if (
        !binding?.constant ||
        !binding.path.isVariableDeclarator() ||
        seen.has(binding)
      )
        return null;
      return strings(
        binding.path.scope,
        binding.path.node.init,
        new Set([...seen, binding]),
        substitutions,
      );
    }
    if (n.type === "TemplateLiteral") {
      let values = [""];
      for (let i = 0; i < n.quasis.length; i++) {
        values = values.map(
          (value) =>
            value + (n.quasis[i].value.cooked ?? n.quasis[i].value.raw),
        );
        if (i < n.expressions.length) {
          const next = strings(scope, n.expressions[i], seen, substitutions);
          if (!next || values.length * next.length > 32) return null;
          values = values.flatMap((value) => next.map((part) => value + part));
        }
      }
      return values;
    }
    if (n.type === "BinaryExpression" && n.operator === "+") {
      const a = strings(scope, n.left, seen, substitutions),
        b = strings(scope, n.right, seen, substitutions);
      return a && b && a.length * b.length <= 32
        ? a.flatMap((x) => b.map((y) => x + y))
        : null;
    }
    return null;
  }
  function request(mod, owner, scope, n, expression, relation, evidence) {
    const values = strings(scope, expression);
    requests.push({
      owner,
      services: values,
      relation,
      evidence,
      source_file: mod.name,
      source_location: `L${n.loc?.start.line ?? mod.source.slice(0, n.start).split("\n").length}`,
      expression: mod.source
        .slice(expression?.start ?? n.start, expression?.end ?? n.end)
        .slice(0, 200),
      confidence: !values
        ? "AMBIGUOUS"
        : relation === "configures_service" || values.length > 1
          ? "INFERRED"
          : "EXTRACTED",
      reason: values
        ? null
        : "dynamic endpoint expression; no interprocedural value propagation",
    });
  }
  function inspectCall(mod, scope, n, owner) {
    const target = targetOf(mod, scope, n.callee);
    const line =
      n.loc?.start.line ?? mod.source.slice(0, n.start).split("\n").length;
    if (target?.id) {
      addEdge(
        owner,
        target.id,
        target.conditional ? "default_callback" : "calls",
        mod.name,
        line,
        target.conditional || target.candidate ? "INFERRED" : "EXTRACTED",
      );
      if (target.module === mod.name && !target.conditional)
        localCalls.push({ mod, owner, scope, n, target: target.id });
    }
    if (
      verifiedCall &&
      verifiedPrefix &&
      target?.module === serviceFile &&
      target.exported === "call"
    ) {
      request(
        mod,
        owner,
        scope,
        n,
        n.arguments[0],
        "calls_http",
        "service.call -> getUrl() + service -> fetch",
      );
      if (!strings(scope, n.arguments[0]))
        dynamicCalls.push({ mod, owner, scope, n, expression: n.arguments[0] });
    }
    if (
      verifiedHelper &&
      verifiedPrefix &&
      target?.module === helperFile &&
      target.exported === "default"
    ) {
      const properties =
        n.arguments[0]?.type === "ObjectExpression"
          ? n.arguments[0].properties
          : [];
      const property = properties.find(
        (p) => (p.key?.name ?? p.key?.value) === "service",
      );
      // A later spread or duplicate property could overwrite the configuration.
      if (
        property &&
        !properties.some((p) => p.type === "SpreadElement") &&
        properties.filter((p) => (p.key?.name ?? p.key?.value) === "service")
          .length === 1
      ) {
        const callback =
          targetOf(mod, scope, property.value)?.id ??
          mod.functions.get(property.value);
        if (callback)
          addEdge(
            owner,
            callback,
            "passes_callback",
            mod.name,
            line,
            "INFERRED",
          );
        else if (
          !["ArrowFunctionExpression", "FunctionExpression"].includes(
            property.value.type,
          )
        ) {
          request(
            mod,
            owner,
            scope,
            n,
            property.value,
            "configures_service",
            "ServiceHelper.service configuration (potential refresh)",
          );
        }
      }
    }
    for (const argument of n.arguments ?? []) {
      const callback = targetOf(mod, scope, argument);
      if (callback?.id)
        addEdge(
          owner,
          callback.id,
          "passes_callback",
          mod.name,
          line,
          "INFERRED",
        );
    }
  }
  function inspectUrl(mod, scope, n, owner) {
    if (n.type !== "TemplateLiteral") return;
    for (let i = 0; i < n.expressions.length; i++) {
      const e = n.expressions[i];
      if (e.type !== "CallExpression" || e.arguments.length) continue;
      const target = targetOf(mod, scope, e.callee);
      if (
        !verifiedPrefix ||
        target?.module !== serviceFile ||
        target.exported !== "getUrl"
      )
        continue;
      const tail = n.quasis[i + 1]?.value.cooked ?? "";
      const endpoint = tail.split(/[?/#]/)[0];
      if (
        !endpoint ||
        !/^[A-Za-z_$][\w$]*(\.[A-Za-z_$][\w$]*)*$/.test(endpoint)
      ) {
        requests.push({
          owner,
          services: null,
          relation: "references_resource",
          source_file: mod.name,
          source_location: `L${n.loc?.start.line ?? mod.source.slice(0, n.start).split("\n").length}`,
          expression: mod.source.slice(n.start, n.end).slice(0, 200),
          reason: "dynamic resource path",
          confidence: "AMBIGUOUS",
        });
      } else
        requests.push({
          owner,
          services: [endpoint],
          relation: "references_resource",
          evidence:
            "getUrl() default admin/services/ prefix; URL construction, not observed traffic",
          source_file: mod.name,
          source_location: `L${n.loc?.start.line ?? mod.source.slice(0, n.start).split("\n").length}`,
          expression: mod.source.slice(n.start, n.end).slice(0, 200),
          confidence: "EXTRACTED",
        });
    }
  }
  for (const mod of modules.values()) {
    for (const tree of mod.trees)
      traverse(tree, {
        CallExpression(p) {
          const f = p.getFunctionParent();
          inspectCall(
            mod,
            p.scope,
            p.node,
            f ? mod.functions.get(f.node) : mod.id,
          );
        },
        TemplateLiteral(p) {
          const f = p.getFunctionParent();
          inspectUrl(
            mod,
            p.scope,
            p.node,
            f ? mod.functions.get(f.node) : mod.id,
          );
        },
      });
    if (!mod.fragment) continue;
    const scope = mod.programScopes.at(-1);
    function templateExpression(n, blocked) {
      try {
        const padding = mod.source.slice(0, n.start).replace(/[^\r\n]/g, " ");
        const expression = babel.parseExpression(
          padding + mod.source.slice(n.start, n.end),
          options,
        );
        const ast = {
          type: "File",
          program: {
            type: "Program",
            sourceType: "module",
            directives: [],
            body: [{ type: "ExpressionStatement", expression }],
          },
        };
        const fallback = (local) => ({
          getBinding: (name) =>
            local.getBinding(name) ??
            (blocked.has(name) ? null : scope?.getBinding(name)),
        });
        traverse(ast, {
          Function(p) {
            const line = p.node.loc.start.line;
            const fid = addNode(
              `${mod.id}@${p.node.start}`,
              `template callback@${line}`,
              "function",
              mod.name,
              line,
            );
            mod.functions.set(p.node, fid);
            mod.functionScopes.set(p.node, p.scope);
            const outer = p.findParent((q) => q.isFunction());
            addEdge(
              outer ? mod.functions.get(outer.node) : mod.id,
              fid,
              "contains",
              mod.name,
              line,
            );
          },
          CallExpression(p) {
            const f = p.getFunctionParent();
            inspectCall(
              mod,
              fallback(p.scope),
              p.node,
              f ? mod.functions.get(f.node) : mod.id,
            );
          },
          TemplateLiteral(p) {
            const f = p.getFunctionParent();
            inspectUrl(
              mod,
              fallback(p.scope),
              p.node,
              f ? mod.functions.get(f.node) : mod.id,
            );
          },
        });
      } catch (error) {
        warnings.push({
          source_file: mod.name,
          line: n.loc?.start.line,
          reason: "template expression parse error",
          message: error.message,
        });
      }
    }
    // Template-local bindings are conservatively blocked, including each/snippet/await/let/const.
    function walk(n, blocked = new Set()) {
      if (!n || typeof n !== "object") return;
      if (Array.isArray(n)) {
        for (const child of n) walk(child, blocked);
        return;
      }
      if (
        [
          "ObjectPattern",
          "ArrayPattern",
          "RestElement",
          "AssignmentPattern",
        ].includes(n.type)
      )
        return;
      if (n.type === "VariableDeclarator") {
        walk(n.init, blocked);
        return;
      }
      if (
        types.isExpression(n) ||
        n.type === "Literal" ||
        n.type === "ChainExpression"
      ) {
        templateExpression(n, blocked);
        return;
      }
      const next = new Set(blocked);
      function block(pattern) {
        if (!pattern || typeof pattern !== "object") return;
        if (pattern.type === "Identifier") next.add(pattern.name);
        else
          for (const value of Object.values(pattern))
            if (typeof value === "object") {
              if (Array.isArray(value)) value.forEach(block);
              else block(value);
            }
      }
      for (const key of ["context", "value", "error"])
        if (["EachBlock", "AwaitBlock"].includes(n.type)) block(n[key]);
      if (n.type === "EachBlock" && n.index) next.add(n.index);
      if (n.type === "SnippetBlock") {
        block(n.expression);
        n.parameters.forEach(block);
      }
      if (n.type === "Fragment")
        for (const child of n.nodes ?? []) {
          if (child.type === "ConstTag" || child.type === "DeclarationTag")
            for (const d of child.declaration.declarations) block(d.id);
        }
      for (const a of n.attributes ?? [])
        if (a.type === "LetDirective") {
          next.add(a.name);
          block(a.expression);
        }
      const safeScope = {
        getBinding: (name) => (next.has(name) ? null : scope?.getBinding(name)),
      };
      if (n.type === "Component" && !next.has(n.name)) {
        const target = targetOf(mod, safeScope, {
          type: "Identifier",
          name: n.name,
        });
        if (target?.id)
          addEdge(
            mod.id,
            target.id,
            "renders",
            mod.name,
            n.name_loc.start.line,
          );
      }
      for (const [key, child] of Object.entries(n))
        if (
          !["loc", "name_loc", "comments"].includes(key) &&
          typeof child === "object"
        )
          walk(child, next);
    }
    walk(mod.fragment);
  }
  // One local call boundary only, retaining the caller context. Do not union all
  // parameter values into a shared callee, which would attribute services to wrong callers.
  for (const call of localCalls) {
    const fn = [...call.mod.functions].find(
      ([, id]) => id === call.target,
    )?.[0];
    if (!fn) continue;
    const substitutions = new Map(),
      fnScope = call.mod.functionScopes.get(fn);
    fn.params.forEach((param, i) => {
      const name =
        param.type === "Identifier"
          ? param.name
          : param.type === "AssignmentPattern"
            ? param.left.name
            : null;
      const values = strings(call.scope, call.n.arguments[i]);
      if (name && values) substitutions.set(fnScope.getBinding(name), values);
    });
    for (const rpc of dynamicCalls.filter((r) => r.owner === call.target)) {
      const values = strings(
        rpc.scope,
        rpc.expression,
        new Set(),
        substitutions,
      );
      if (!values) continue;
      requests.push({
        owner: call.owner,
        services: values,
        relation: "calls_http",
        confidence: "INFERRED",
        source_file: call.mod.name,
        source_location: `L${call.n.loc.start.line}`,
        expression: call.mod.source
          .slice(call.n.start, call.n.end)
          .slice(0, 200),
        evidence: `One local argument substitution into RPC at ${rpc.mod.name}:L${rpc.n.loc.start.line}`,
        rpc_source: `${rpc.mod.name}:L${rpc.n.loc.start.line}`,
        derived: true,
      });
    }
  }
  const configurations = requests.filter(
    (r) => r.relation === "configures_service" && r.services,
  );
  for (const mod of modules.values())
    for (const imported of mod.imports) {
      const state = modules.get(imported.target);
      if (!state?.defaultConfigured) continue;
      for (const config of configurations.filter((r) => r.owner === state.id)) {
        requests.push({
          ...config,
          owner: mod.id,
          source_file: mod.name,
          source_location: `L${imported.line}`,
          evidence: `Imported ServiceHelper state; potential refresh configured at ${config.source_file}:${config.source_location}`,
          configuration_source: `${config.source_file}:${config.source_location}`,
          confidence: "INFERRED",
          derived: true,
        });
      }
    }
  return {
    nodes,
    edges,
    requests,
    warnings,
    parsers: {
      svelte: svelte.VERSION,
      babel: requireWeb("@babel/parser/package.json").version,
    },
    files: modules.size,
    components: [...modules.keys()].filter((n) => n.endsWith(".svelte")).length,
    parse_errors: warnings.filter((w) => w.reason === "parse error").length,
  };
}

module.exports = { extract };
if (require.main === module) {
  const [root, dependencyRoot, manifest, output] = process.argv.slice(2);
  fs.writeFileSync(
    output,
    JSON.stringify(
      extract(
        root,
        dependencyRoot,
        JSON.parse(fs.readFileSync(manifest, "utf8")),
      ),
      null,
      2,
    ) + "\n",
  );
}
