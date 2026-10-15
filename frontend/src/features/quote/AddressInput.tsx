import { MapPin } from 'lucide-react'
import { useEffect, useId, useState } from 'react'

type Suggestion = { label: string }

/**
 * Adresse avec suggestions pendant la saisie (service public de geocodage de l'IGN, gratuit, sans cle).
 * Le serveur verifie de nouveau l'adresse et calcule lui-meme la distance de livraison.
 */
export function AddressInput({ value, onChange, error }: { value: string; onChange: (value: string) => void; error?: string }) {
  const id = useId()
  const [suggestions, setSuggestions] = useState<Suggestion[]>([])
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (value.trim().length < 5) return
    const controller = new AbortController()
    const timer = window.setTimeout(() => {
      fetch(`https://data.geopf.fr/geocodage/search?autocomplete=1&limit=5&q=${encodeURIComponent(value)}`, { signal: controller.signal })
        .then((response) => (response.ok ? response.json() : { features: [] }))
        .then((body: { features: { properties: Suggestion }[] }) => setSuggestions(body.features.map((feature) => ({ label: feature.properties.label }))))
        .catch(() => setSuggestions([]))
    }, 300)
    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [value])

  const shown = open && value.trim().length >= 5 ? suggestions.filter((s) => s.label !== value) : []

  return (
    <div className="relative">
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-slate-700">
        Adresse de l’événement
      </label>
      <input
        id={id}
        value={value}
        autoComplete="off"
        onChange={(e) => {
          onChange(e.target.value)
          setOpen(true)
        }}
        onBlur={() => window.setTimeout(() => setOpen(false), 150)}
        placeholder="Numéro, rue, ville"
        aria-invalid={Boolean(error)}
        className={`w-full rounded-xl border bg-white px-3.5 py-2.5 text-sm outline-none focus:ring-4 ${error ? 'border-red-400 focus:ring-red-100' : 'border-slate-300 focus:border-brand-600 focus:ring-brand-100'}`}
      />
      {shown.length > 0 && (
        <ul role="listbox" aria-label="Adresses proposées" className="absolute z-20 mt-1 w-full overflow-hidden rounded-xl bg-white shadow-lg ring-1 ring-stone-200">
          {shown.map((suggestion) => (
            <li key={suggestion.label} role="option" aria-selected={false}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange(suggestion.label)
                  setOpen(false)
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-brand-50"
              >
                <MapPin className="size-4 shrink-0 text-brand-700" aria-hidden />
                {suggestion.label}
              </button>
            </li>
          ))}
        </ul>
      )}
      {error && <p className="mt-1.5 text-sm text-red-700">{error}</p>}
    </div>
  )
}
