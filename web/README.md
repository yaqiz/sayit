# Web Image UI

## Start

```powershell
cd web
npm install
npm run dev
```

## API

By default the page runs in mock mode and returns a generated image URL so the UI can be tested immediately.

To connect a real API, create `web/.env`:

```powershell
VITE_IMAGE_API_URL=https://your-api.example.com/generate-image
```

The page sends a `POST` JSON request with:

- `model`
- `mode`
- `prompt`
- `negativePrompt`
- `aspectRatio`
- `clarity`
- `quantity`

The response can be one of these shapes:

```json
{ "imageUrl": "https://..." }
```

```json
{ "url": "https://..." }
```

```json
{ "data": [{ "url": "https://..." }] }
```
