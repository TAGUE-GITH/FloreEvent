import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { addItem, emptySelection, itemCount, readStoredSelection, setQuantity } from '../../lib/selection'
import type { Selection } from '../../lib/selection'

const STORAGE_KEY = 'floreevent_selection'

type SelectionValue = {
  selection: Selection
  count: number
  add: (item: Parameters<typeof addItem>[1], quantity: number) => void
  setLineQuantity: (itemId: number, quantity: number) => void
  update: (changes: Partial<Omit<Selection, 'lines'>>) => void
  clear: () => void
}

const SelectionContext = createContext<SelectionValue | null>(null)

/** Le stockage du navigateur peut etre bloque (navigation privee) : la selection marche alors sans etre gardee. */
function load(): Selection {
  try {
    return readStoredSelection(localStorage.getItem(STORAGE_KEY))
  } catch {
    return emptySelection()
  }
}

/** Selection de materiel du visiteur (son devis en cours), gardee dans le navigateur d'une page a l'autre. */
export function SelectionProvider({ children }: { children: ReactNode }) {
  const [selection, setSelection] = useState<Selection>(load)

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(selection))
    } catch {
      // Stockage indisponible : la selection reste en memoire
    }
  }, [selection])

  const add = useCallback((item: Parameters<typeof addItem>[1], quantity: number) => setSelection((current) => addItem(current, item, quantity)), [])
  const setLineQuantity = useCallback((itemId: number, quantity: number) => setSelection((current) => setQuantity(current, itemId, quantity)), [])
  const update = useCallback((changes: Partial<Omit<Selection, 'lines'>>) => setSelection((current) => ({ ...current, ...changes })), [])
  const clear = useCallback(() => setSelection(emptySelection()), [])

  const value = useMemo(
    () => ({ selection, count: itemCount(selection), add, setLineQuantity, update, clear }),
    [selection, add, setLineQuantity, update, clear],
  )
  return <SelectionContext.Provider value={value}>{children}</SelectionContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSelection(): SelectionValue {
  const value = useContext(SelectionContext)
  if (!value) throw new Error('useSelection doit etre utilise dans SelectionProvider')
  return value
}
