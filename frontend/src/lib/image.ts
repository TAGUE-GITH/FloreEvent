// Reduction d'une photo dans le navigateur avant l'envoi : rapide sur une connexion mobile lente,
// et le serveur ne stocke que des images legeres (JPEG, cote maximale en pixels).

/** Dimensions reduites en gardant les proportions. */
export function fitWithin(width: number, height: number, max: number): { width: number; height: number } {
  if (width <= max && height <= max) return { width, height }
  const ratio = Math.min(max / width, max / height)
  return { width: Math.round(width * ratio), height: Math.round(height * ratio) }
}

export async function resizeImage(file: File, max = 512, quality = 0.85): Promise<string> {
  if (!file.type.startsWith('image/')) throw new Error('Choisissez une image (JPEG ou PNG).')
  const bitmap = await createImageBitmap(file)
  const size = fitWithin(bitmap.width, bitmap.height, max)
  const canvas = document.createElement('canvas')
  canvas.width = size.width
  canvas.height = size.height
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Votre navigateur ne peut pas préparer cette image.')
  // Fond blanc : une image PNG transparente ne devient pas noire en JPEG
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, size.width, size.height)
  context.drawImage(bitmap, 0, 0, size.width, size.height)
  return canvas.toDataURL('image/jpeg', quality)
}
