# Convertigo Studio Web — Developer Notes

## Workflow

```bash
npm install
npm run dev
```

## Device Frames

Preview bezels are stored as WebP files in `static/bezels/<id>.webp`, with matching thumbnails in `static/bezels/thumbnails/<id>.webp`. The list of supported devices is declared in `config/device-catalog.json`; `src/lib/dashboard/Bezels.js` is **generated**—never edit it manually.

### Refreshing devices (every ~6 months)

1. Update `config/device-catalog.json` (add/remove devices, adjust viewports). The `source` field must point to an asset shipped in this repo (`assets/bezel-sources/...`) or to a PNG inside [mockup-device-frames](https://github.com/jamesjingyi/mockup-device-frames).
2. Make sure every `source` path exists. For external assets, confirm the file is present in that repository.
3. Run the importer:

   ```bash
   npm run update:bezels
   ```

   This command will:
   - clone/update `.cache/mockup-device-frames`,
   - regenerate WebP bezels and thumbnails,
   - rebuild `src/lib/dashboard/Bezels.js` with correct dimensions,
   - remove orphan bezel/thumbnail files.

   When you only need to tweak thumbnail rendering without touching the source PNGs, run:

   ```bash
   npm run generate:thumbnails
   ```

   This re-renders every entry in `static/bezels/thumbnails/` using the already-generated bezels and the metrics stored in `src/lib/dashboard/Bezels.js`.

### Prompt template for GPT

```
You help maintain config/device-catalog.json for the Convertigo Studio preview. Based on smartphone/tablet/desktop sales trends from the last 6 months, select the most relevant models. For each model, verify the PNG bezel exists in github.com/jamesjingyi/mockup-device-frames (under Exports/…), or point to assets/bezel-sources when we host the file ourselves. Return only JSON with: id (CamelCase without spaces), title, type (phone/tablet/desktop), viewport.width/height (CSS px) and source (relative path as mentioned). Output only the JSON array.
```

Paste the resulting JSON into `config/device-catalog.json`, then run `npm run update:bezels`.

### Notes

- All bezels and thumbnails are WebP (modern browsers support it natively).
- Original PNGs we host ourselves live in `assets/bezel-sources/`.
- Thumbnails already include a neutral background, ready to render in the UI.
- The importer purges unused files automatically, so `static/bezels` stays clean.

## Misc

To add new categories (desktops, etc.), simply append entries in `config/device-catalog.json` and re-run the importer.
