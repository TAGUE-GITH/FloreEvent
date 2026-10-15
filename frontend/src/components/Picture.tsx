import { photoSrcSet, photoUrl } from '../lib/photos'
import type { Photo } from '../lib/photos'

/** Photo responsive : le navigateur charge la taille adaptee a l'ecran. */
export function Picture({ photo, ratio, sizes, className = '', eager = false }: { photo: Photo; ratio: number; sizes: string; className?: string; eager?: boolean }) {
  return (
    <img
      src={photoUrl(photo, 960, Math.round(960 / ratio))}
      srcSet={photoSrcSet(photo, [480, 960, 1600], ratio)}
      sizes={sizes}
      alt={photo.alt}
      loading={eager ? 'eager' : 'lazy'}
      fetchPriority={eager ? 'high' : 'auto'}
      className={`bg-stone-200 object-cover ${className}`}
    />
  )
}
