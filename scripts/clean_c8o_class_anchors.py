#!/usr/bin/env python3
"""
Clean accidental Convertigo priority anchors from YAML class attributes.

The generated Ionic HTML uses class<priority> tokens as round-trip anchors. Those
tokens must stay in generated HTML, but they must not be persisted as semantic
class attributes in the Convertigo YAML model. When possible, this script also
migrates simple .class<priority> rules from a generated C8oIonicStyle block back
to the targeted bean as a UIStyle child.
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path


OBJECT_HEADER_RE = re.compile(
    r"^(?P<indent>\s*)↓(?P<name>.*?) \[(?P<component>.+)-(?P<priority>\d+)\]:\s*$"
)
ATTR_NAME_CLASS_RE = re.compile(r"^\s*attrName:\s*['\"]?class['\"]?\s*$")
PRIORITY_TOKEN_RE = re.compile(r"^class\d+$")
VALUE_LINE_RE = re.compile(
    r"^(?P<prefix>\s*-\s*(?:MobileSmartSourceType|→→):\s*)(?P<value>.*?)(?P<newline>\r?\n?)$"
)
CSS_CLASS_SELECTOR_RE = re.compile(r"\.class(?P<priority>\d+)")
MAPPABLE_CLASS_SELECTOR_RE = re.compile(
    r"\.class(?P<priority>\d+)(?P<suffix>(?::hover|::part\([^)]+\))?)"
)


@dataclass
class ObjectBlock:
    path: Path
    start: int
    end: int
    indent: int
    name: str
    component: str
    priority: str
    parent_priority: str | None


@dataclass
class ClassAttribute:
    block: ObjectBlock
    parent_priority: str
    tokens: list[str]


@dataclass
class CssRule:
    start: int
    end: int
    selector: str
    body: str
    class_tokens: list[str]
    suffixes: list[str]


@dataclass
class FileReport:
    path: Path
    class_tokens_removed: int = 0
    class_attributes_removed: int = 0
    css_rules_migrated: int = 0
    orphan_css_rules_dropped: int = 0
    ui_styles_created: int = 0
    style_blocks_normalized: int = 0
    ionic_style_blocks_removed: int = 0
    ionic_style_blocks_updated: int = 0
    skipped_css_rules: list[str] = field(default_factory=list)

    def changed(self) -> bool:
        return any(
            [
                self.class_tokens_removed,
                self.class_attributes_removed,
                self.css_rules_migrated,
                self.orphan_css_rules_dropped,
                self.ui_styles_created,
                self.style_blocks_normalized,
                self.ionic_style_blocks_removed,
                self.ionic_style_blocks_updated,
            ]
        )


def yaml_files(project: Path) -> list[Path]:
    files: list[Path] = []
    root_yaml = project / "c8oProject.yaml"
    if root_yaml.is_file():
        files.append(root_yaml)
    mirror = project / "_c8oProject"
    if mirror.is_dir():
        files.extend(sorted(mirror.rglob("*.yaml")))
    return files


def parse_objects(path: Path, lines: list[str]) -> list[ObjectBlock]:
    objects: list[ObjectBlock] = []
    stack: list[ObjectBlock] = []

    for line_no, line in enumerate(lines):
        match = OBJECT_HEADER_RE.match(line)
        if not match:
            continue

        indent = len(match.group("indent"))
        while stack and indent <= stack[-1].indent:
            stack[-1].end = line_no
            stack.pop()

        parent_priority = stack[-1].priority if stack else None
        obj = ObjectBlock(
            path=path,
            start=line_no,
            end=len(lines),
            indent=indent,
            name=match.group("name"),
            component=match.group("component"),
            priority=match.group("priority"),
            parent_priority=parent_priority,
        )
        objects.append(obj)
        stack.append(obj)

    return objects


def matching_quote(value: str) -> str | None:
    if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
        return value[0]
    return None


def clean_class_text(text: str) -> tuple[str, list[str]]:
    tokens = text.split()
    removed = [token for token in tokens if PRIORITY_TOKEN_RE.fullmatch(token)]
    kept = [token for token in tokens if not PRIORITY_TOKEN_RE.fullmatch(token)]
    return " ".join(kept), removed


def clean_value(raw_value: str) -> tuple[str, list[str], bool]:
    suffix = ""
    value = raw_value.rstrip()
    if value != raw_value:
        suffix = raw_value[len(value):]

    quote = matching_quote(value)
    inner = value[1:-1] if quote else value

    if inner.startswith("plain:"):
        cleaned, removed = clean_class_text(inner[len("plain:"):])
        if not removed:
            return raw_value, [], False
        inner = "plain:" + cleaned
        is_empty = cleaned == ""
    else:
        cleaned, removed = clean_class_text(inner)
        if not removed:
            return raw_value, [], False
        inner = cleaned
        is_empty = cleaned == ""

    if quote:
        return quote + inner + quote + suffix, removed, is_empty
    return inner + suffix, removed, is_empty


def clean_value_line(line: str) -> tuple[str, list[str], bool]:
    match = VALUE_LINE_RE.match(line)
    if not match:
        return line, [], False
    new_value, removed, is_empty = clean_value(match.group("value"))
    if not removed:
        return line, [], False
    return f"{match.group('prefix')}{new_value}{match.group('newline')}", removed, is_empty


def block_has_class_attr(lines: list[str], block: ObjectBlock) -> bool:
    return any(ATTR_NAME_CLASS_RE.match(line) for line in lines[block.start : block.end])


def extract_priority_tokens_from_class_attr(lines: list[str], block: ObjectBlock) -> list[str]:
    tokens: list[str] = []
    for line in lines[block.start : block.end]:
        match = VALUE_LINE_RE.match(line)
        if not match:
            continue
        value = match.group("value").strip()
        quote = matching_quote(value)
        inner = value[1:-1] if quote else value
        if inner.startswith("plain:"):
            inner = inner[len("plain:") :]
        for token in inner.split():
            if PRIORITY_TOKEN_RE.fullmatch(token):
                tokens.append(token)
    return tokens


def collect_class_attributes(lines: list[str], objects: list[ObjectBlock]) -> list[ClassAttribute]:
    attrs: list[ClassAttribute] = []
    for obj in objects:
        if obj.component != "ngx.components.UIAttribute":
            continue
        if obj.parent_priority is None:
            continue
        if not block_has_class_attr(lines, obj):
            continue
        tokens = extract_priority_tokens_from_class_attr(lines, obj)
        if tokens:
            attrs.append(ClassAttribute(obj, obj.parent_priority, tokens))
    return attrs


def content_indent(lines: list[str], start: int, end: int) -> int:
    for line in lines[start:end]:
        if line.strip():
            return len(line) - len(line.lstrip())
    return 0


def extract_scalar_content(lines: list[str], block: ObjectBlock) -> tuple[int, int, int, str] | None:
    arrow_line = None
    for index in range(block.start, block.end):
        if "→: |" in lines[index]:
            arrow_line = index
            break
    if arrow_line is None:
        return None

    scalar_start = arrow_line + 1
    scalar_end = block.end
    indent = content_indent(lines, scalar_start, scalar_end)
    raw_lines: list[str] = []
    for line in lines[scalar_start:scalar_end]:
        text = line
        if indent and text.startswith(" " * indent):
            text = text[indent:]
        raw_lines.append(text.rstrip("\n"))

    content = "\n".join(raw_lines)
    if content.startswith("'") and content.endswith("'"):
        content = content[1:-1]
    return scalar_start, scalar_end, indent, content


def render_scalar_content(content: str, indent: int) -> list[str]:
    content = content.rstrip("\n")
    if not content:
        return [(" " * indent) + "''\n"]

    rendered: list[str] = []
    for index, line in enumerate(content.splitlines()):
        if index == 0:
            line = "'" + line
        rendered.append((" " * indent) + line + "\n")
    rendered.append((" " * indent) + "'\n")
    return rendered


def parse_simple_class_rules(css: str) -> list[CssRule]:
    rules: list[CssRule] = []
    lines = css.splitlines(keepends=True)
    offsets: list[int] = []
    offset = 0
    for line in lines:
        offsets.append(offset)
        offset += len(line)

    line_index = 0
    while line_index < len(lines):
        stripped = lines[line_index].lstrip()
        if not stripped.startswith(".class"):
            line_index += 1
            continue

        start_line = line_index
        depth = 0
        saw_open = False
        end_line = line_index
        while end_line < len(lines):
            for char in lines[end_line]:
                if char == "{":
                    depth += 1
                    saw_open = True
                elif char == "}":
                    depth -= 1
            end_line += 1
            if saw_open and depth == 0:
                break

        if not saw_open or depth != 0:
            line_index = start_line + 1
            continue

        rule_text = "".join(lines[start_line:end_line])
        open_index = rule_text.find("{")
        close_index = rule_text.rfind("}")
        if open_index == -1 or close_index == -1 or close_index < open_index:
            line_index = end_line
            continue

        selector = rule_text[:open_index].strip()
        body = rule_text[open_index + 1 : close_index]
        selectors = [part.strip() for part in selector.split(",")]
        class_tokens: list[str] = []
        suffixes: list[str] = []
        simple = True
        for part in selectors:
            match = MAPPABLE_CLASS_SELECTOR_RE.fullmatch(part)
            if not match:
                simple = False
                break
            class_tokens.append("class" + match.group("priority"))
            suffixes.append(match.group("suffix") or "")

        if simple and class_tokens:
            rules.append(
                CssRule(
                    offsets[start_line],
                    offsets[end_line - 1] + len(lines[end_line - 1]),
                    selector,
                    body,
                    class_tokens,
                    suffixes,
                )
            )

        line_index = end_line

    return rules


def normalize_style(text: str) -> str:
    return re.sub(r"\s+", "", text).replace(";", "")


def style_body_for_selector(rule: CssRule, selector_index: int) -> str:
    suffix = rule.suffixes[selector_index]
    body = rule.body.strip()
    if not suffix:
        return rule.body
    return f"&{suffix} {{\n{body}\n}}"


def existing_style_bodies(lines: list[str], objects: list[ObjectBlock], parent: ObjectBlock) -> set[str]:
    bodies: set[str] = set()
    for obj in objects:
        if obj.parent_priority != parent.priority:
            continue
        if obj.component != "ngx.components.UIStyle":
            continue
        extracted = extract_scalar_content(lines, obj)
        if extracted is None:
            continue
        bodies.add(normalize_style(extracted[3]))
    return bodies


def next_priority(used: set[str]) -> str:
    value = max((int(item) for item in used if item.isdigit()), default=1777000000000) + 1
    while str(value) in used:
        value += 1
    used.add(str(value))
    return str(value)


def style_block_lines(indent: int, name: str, priority: str, body: str) -> list[str]:
    base = " " * indent
    nested = " " * (indent + 2)
    content_indent_value = indent + 10
    body = body.strip()
    if body and not body.endswith("\n"):
        body += "\n"
    rendered_body = render_scalar_content(body, content_indent_value)
    return [
        f"{base}↓{name} [ngx.components.UIStyle-{priority}]: \n",
        f"{nested}styleContent: \n",
        f"{nested}  - xmlizable: \n",
        f"{nested}    - ↑classname: com.twinsoft.convertigo.beans.common.FormatedContent\n",
        f"{nested}    - com.twinsoft.convertigo.beans.common.FormatedContent: \n",
        f"{nested}      →: |\n",
        *rendered_body,
    ]


def expected_style_content_indent(style_indent: int) -> int:
    return style_indent + 10


def apply_operations(lines: list[str], operations: list[tuple[int, int, list[str]]]) -> None:
    for start, end, replacement in sorted(
        operations,
        key=lambda item: (item[0], item[1] - item[0]),
        reverse=True,
    ):
        lines[start:end] = replacement


def remove_css_ranges(css: str, ranges: list[tuple[int, int]]) -> str:
    for start, end in sorted(ranges, reverse=True):
        css = css[:start] + css[end:]
    return css.strip()


def is_css_effectively_empty(css: str) -> bool:
    stripped = css.strip()
    if stripped in ("", ";"):
        return True
    return False


def cleanup_remaining_css(css: str) -> str:
    previous = None
    while previous != css:
        previous = css
        css = re.sub(r"(?m)^\s*@media[^{]+\{\s*\}\s*", "", css)
    return css.strip()


def clean_file(path: Path, used_priorities: set[str]) -> tuple[str | None, FileReport]:
    original = path.read_text(encoding="utf-8")
    lines = original.splitlines(keepends=True)
    report = FileReport(path)

    objects = parse_objects(path, lines)
    objects_by_priority = {obj.priority: obj for obj in objects}
    class_attrs = collect_class_attributes(lines, objects)
    token_targets: dict[str, set[str]] = {}
    for attr in class_attrs:
        for token in attr.tokens:
            token_targets.setdefault(token, set()).add(attr.parent_priority)

    operations: list[tuple[int, int, list[str]]] = []

    for obj in objects:
        if obj.component != "ngx.components.UIAttribute":
            continue
        if not block_has_class_attr(lines, obj):
            continue

        block_empty = False
        block_changed = False
        block_replacements: list[tuple[int, str]] = []
        for index in range(obj.start, obj.end):
            new_line, removed, is_empty = clean_value_line(lines[index])
            if not removed:
                continue
            block_replacements.append((index, new_line))
            block_changed = True
            report.class_tokens_removed += len(removed)
            if is_empty:
                block_empty = True

        if block_changed and block_empty:
            operations.append((obj.start, obj.end, []))
            report.class_attributes_removed += 1
        elif block_changed:
            for index, new_line in block_replacements:
                operations.append((index, index + 1, [new_line]))

    style_insertions: dict[int, list[tuple[int, list[str]]]] = {}

    existing_bodies_cache: dict[str, set[str]] = {}
    for style_obj in objects:
        if style_obj.component != "ngx.components.UIStyle":
            continue

        extracted = extract_scalar_content(lines, style_obj)
        if extracted is None:
            continue

        scalar_start, scalar_end, indent, css = extracted
        if style_obj.name.startswith("StyleFromIonic"):
            expected_indent = expected_style_content_indent(style_obj.indent)
            rendered = render_scalar_content(css, expected_indent)
            if indent != expected_indent or lines[scalar_start:scalar_end] != rendered:
                operations.append((scalar_start, scalar_end, rendered))
                report.style_blocks_normalized += 1
            continue

        if style_obj.name != "C8oIonicStyle":
            continue

        rules = parse_simple_class_rules(css)
        remove_ranges: list[tuple[int, int]] = []

        for rule in rules:
            target_entries: list[tuple[int, str]] = []
            unresolved_tokens: list[str] = []
            for selector_index, token in enumerate(rule.class_tokens):
                targets = token_targets.get(token)
                if targets and len(targets) == 1:
                    target_entries.append((selector_index, next(iter(targets))))
                    continue

                priority = token.removeprefix("class")
                target = objects_by_priority.get(priority)
                if target is not None and target.component not in {
                    "ngx.components.UIStyle",
                    "ngx.components.UIAttribute",
                }:
                    target_entries.append((selector_index, priority))
                    continue

                unresolved_tokens.append(token)

            for selector_index, target_priority in target_entries:
                target = objects_by_priority[target_priority]
                target_body = style_body_for_selector(rule, selector_index)
                normalized_body = normalize_style(target_body)
                existing = existing_bodies_cache.setdefault(
                    target_priority, existing_style_bodies(lines, objects, target)
                )
                if normalized_body in existing:
                    continue
                new_priority = next_priority(used_priorities)
                style_name = "StyleFromIonic" + new_priority
                style_lines = style_block_lines(
                    target.indent + 2,
                    style_name,
                    new_priority,
                    target_body,
                )
                style_insertions.setdefault(target.end, []).append((target.indent, style_lines))
                existing.add(normalized_body)
                report.ui_styles_created += 1

            remove_ranges.append((rule.start, rule.end))
            if target_entries:
                report.css_rules_migrated += 1
            else:
                report.orphan_css_rules_dropped += 1
            if unresolved_tokens:
                report.skipped_css_rules.append(
                    f"{rule.selector}: dropped orphan selector(s) {', '.join(unresolved_tokens)}"
                )

        new_css = cleanup_remaining_css(remove_css_ranges(css, remove_ranges)) if remove_ranges else cleanup_remaining_css(css)
        if remove_ranges or new_css != css.strip():
            if is_css_effectively_empty(new_css):
                operations.append((style_obj.start, style_obj.end, []))
                report.ionic_style_blocks_removed += 1
            else:
                rendered = render_scalar_content(new_css, indent)
                operations.append((scalar_start, scalar_end, rendered))
                report.ionic_style_blocks_updated += 1

    for index, insertions in style_insertions.items():
        inserted_lines: list[str] = []
        for _indent, item_lines in sorted(insertions, key=lambda item: item[0], reverse=True):
            inserted_lines.extend(item_lines)
        operations.append((index, index, inserted_lines))
    apply_operations(lines, operations)

    updated = "".join(lines)
    if updated == original:
        return None, report
    return updated, report


def collect_used_priorities(files: list[Path]) -> set[str]:
    used: set[str] = set()
    for path in files:
        for line in path.read_text(encoding="utf-8").splitlines():
            match = OBJECT_HEADER_RE.match(line)
            if match:
                used.add(match.group("priority"))
    return used


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Clean accidental class<priority> tokens from Convertigo YAML class attributes."
    )
    parser.add_argument("project", type=Path, help="Convertigo project directory")
    parser.add_argument("--dry-run", action="store_true", help="Report changes without writing files")
    parser.add_argument("--verbose", action="store_true", help="Print skipped CSS migration details")
    args = parser.parse_args()

    project = args.project.expanduser().resolve()
    if not project.is_dir():
        print(f"Project directory not found: {project}", file=sys.stderr)
        return 2

    files = yaml_files(project)
    used_priorities = collect_used_priorities(files)
    changed: dict[Path, str] = {}
    reports: list[FileReport] = []

    for path in files:
        updated, report = clean_file(path, used_priorities)
        reports.append(report)
        if updated is not None:
            changed[path] = updated

    if not args.dry_run:
        for path, updated in changed.items():
            path.write_text(updated, encoding="utf-8")

    total_tokens = sum(report.class_tokens_removed for report in reports)
    total_attrs = sum(report.class_attributes_removed for report in reports)
    total_rules = sum(report.css_rules_migrated for report in reports)
    total_orphans = sum(report.orphan_css_rules_dropped for report in reports)
    total_styles = sum(report.ui_styles_created for report in reports)
    total_normalized = sum(report.style_blocks_normalized for report in reports)
    total_blocks_removed = sum(report.ionic_style_blocks_removed for report in reports)
    total_blocks_updated = sum(report.ionic_style_blocks_updated for report in reports)

    print(f"project: {project}")
    print(f"yaml_files_scanned: {len(files)}")
    print(f"files_changed: {len(changed)}")
    print(f"class_priority_tokens_removed: {total_tokens}")
    print(f"empty_class_attributes_removed: {total_attrs}")
    print(f"css_rules_migrated_from_c8oionicstyle: {total_rules}")
    print(f"orphan_css_rules_dropped_from_c8oionicstyle: {total_orphans}")
    print(f"ui_styles_created: {total_styles}")
    print(f"style_blocks_normalized: {total_normalized}")
    print(f"c8oionicstyle_blocks_removed: {total_blocks_removed}")
    print(f"c8oionicstyle_blocks_updated: {total_blocks_updated}")

    if changed:
        print("changed_files:")
        for path in sorted(changed):
            report = next(item for item in reports if item.path == path)
            print(
                f"- {path}: tokens={report.class_tokens_removed}, "
                f"empty_attrs={report.class_attributes_removed}, "
                f"rules={report.css_rules_migrated}, orphan_rules={report.orphan_css_rules_dropped}, "
                f"ui_styles={report.ui_styles_created}, normalized={report.style_blocks_normalized}, "
                f"blocks_removed={report.ionic_style_blocks_removed}, "
                f"blocks_updated={report.ionic_style_blocks_updated}"
            )

    if args.verbose:
        for report in reports:
            if report.skipped_css_rules:
                print(f"skipped_css_rules: {report.path}")
                for item in report.skipped_css_rules:
                    print(f"  - {item}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
