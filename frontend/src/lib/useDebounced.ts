import { useEffect, useState } from 'react'

/** Valeur mise a jour seulement apres une pause de `delay` ms (evite un appel au serveur a chaque touche). */
export function useDebounced<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay)
    return () => window.clearTimeout(timer)
  }, [value, delay])
  return debounced
}
