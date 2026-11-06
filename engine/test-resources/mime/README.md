# Static MIME corpus

The static-file regression test generates HTML, JavaScript, CSS, JSON, SVG, PNG, PDF and unknown binary files, checks the Tika fallback's Content-Type and response bytes, and checks PNG/PDF/SVG/WOFF2 signatures with an unknown extension. Servlet MIME mappings are checked separately for precedence.

The unmodified WOFF2 fixture is IBM Plex Sans Latin (variable weight), copied from the Studio web dependency `@fontsource-variable/ibm-plex-sans`, file `ibm-plex-sans-latin-wght-normal.woff2`. Its original SIL Open Font License and copyright notice are in `OFL.txt`.

Recorded fallback results are identical with Tika Core 3.3.3 and 4.2.0:

| File | Content-Type |
| --- | --- |
| HTML | text/html |
| JavaScript | text/javascript |
| CSS | text/css |
| JSON | application/json |
| SVG | image/svg+xml |
| PNG | image/png |
| PDF | application/pdf |
| WOFF2 | application/octet-stream |
| Unknown binary | application/octet-stream |

PNG, PDF, SVG and WOFF2 give the same results with an unknown extension. The WOFF2 result describes the existing Tika fallback only, not an override of a MIME type supplied by the servlet context.
