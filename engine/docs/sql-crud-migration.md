# Migrate a generated SQL CRUD project

Work on a copy of the project and keep the original export. Select the table on the SQL connector's Design tab. Generation reports transactions with legacy names and no generated table identity, along with any matching sequences. Review their SQL, variables, settings and callers before moving them. The report also identifies legacy operations that the current table metadata would omit.

1. Record the callable names, access and authentication settings, transaction variables, response shape and any changes to the generated NGX shared component, forms and pages.
2. Move the old objects aside in the project copy and generate the transactions, sequence wrappers and NGX interface in the same pass. Transfer reviewed business rules into separate requestables or deliberately customized wrappers.
3. Update the callers together. Map a legacy `order_by` value to the generated sort column and direction inputs. Test stable paging and nullable values. Where compatibility is required, retain a wrapper under the old public name that maps supported inputs to the new contract.
4. Review access to old write operations on tables without a verified key. Retire those endpoints when they should no longer be available. Test existing HTTP clients, the generated UI and the effective permissions before deploying the copy.
