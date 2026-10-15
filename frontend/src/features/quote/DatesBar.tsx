import { CalendarDays } from 'lucide-react'
import { useState } from 'react'
import { Field } from '../../components/ui'
import { todayIso } from '../../lib/dates'
import { rentalDays } from '../../lib/selection'
import { useSelection } from './Selection'

/** Dates de l'evenement, partagees par le catalogue et le devis : les disponibilites en dependent. */
export function DatesBar() {
  const { selection, update } = useSelection()
  // Location au plus tot demain (meme regle que le serveur)
  const [tomorrow] = useState(() => {
    const date = new Date(`${todayIso()}T12:00:00Z`)
    date.setUTCDate(date.getUTCDate() + 1)
    return date.toISOString().slice(0, 10)
  })
  const days = rentalDays(selection.startDate, selection.endDate)
  const inverted = Boolean(selection.startDate && selection.endDate && days === null)

  return (
    <div className="rounded-2xl bg-white p-4 ring-1 ring-stone-200">
      <p className="mb-3 flex items-center gap-2 text-sm font-bold">
        <CalendarDays className="size-4 text-brand-700" aria-hidden />
        Dates de votre événement
      </p>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field
          label="Du"
          type="date"
          min={tomorrow}
          value={selection.startDate}
          onChange={(e) => update({ startDate: e.target.value, endDate: selection.endDate && selection.endDate >= e.target.value ? selection.endDate : e.target.value })}
        />
        <Field label="Au" type="date" min={selection.startDate || tomorrow} value={selection.endDate} onChange={(e) => update({ endDate: e.target.value })} error={inverted ? 'La date de fin doit être après la date de début.' : undefined} />
        <p className="text-sm text-slate-600 sm:col-span-2">{days ? `${days} jour${days > 1 ? 's' : ''} de location` : 'Choisissez vos dates pour voir les disponibilités'}</p>
      </div>
    </div>
  )
}
