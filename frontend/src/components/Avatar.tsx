import { imageUrl } from '../api/client'
import type { User } from '../api/types'

const SIZES = { sm: 'size-9 text-sm', md: 'size-12 text-base', lg: 'size-24 text-2xl' }

/** Photo de profil, ou les initiales quand il n'y en a pas. */
export function Avatar({ user, size = 'sm' }: { user: Pick<User, 'firstName' | 'lastName' | 'avatarUrl'>; size?: keyof typeof SIZES }) {
  const initials = `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}`.toUpperCase()
  if (user.avatarUrl)
    return <img src={imageUrl(user.avatarUrl)} alt="" className={`${SIZES[size]} shrink-0 rounded-full object-cover ring-2 ring-white`} />
  return (
    <span className={`${SIZES[size]} flex shrink-0 items-center justify-center rounded-full bg-brand-100 font-bold text-brand-800`} aria-hidden>
      {initials}
    </span>
  )
}
