import { Clock } from 'lucide-react'
import { useEffect, useState } from 'react'
import { HOURS, openingStatus } from '../lib/hours'

/** Horaires de FloreEvent, avec « Ouvert » ou « Fermé » selon l'heure de Paris (mis a jour chaque minute). */
export function OpeningHours({ className = '' }: { className?: string }) {
  const [status, setStatus] = useState(() => openingStatus())
  useEffect(() => {
    const timer = window.setInterval(() => setStatus(openingStatus()), 60_000)
    return () => window.clearInterval(timer)
  }, [])

  return (
    <div className={className}>
      <p className={`inline-flex items-center gap-2 text-sm font-semibold ${status.open ? 'text-emerald-700' : 'text-red-700'}`}>
        <span className={`size-2 rounded-full ${status.open ? 'bg-emerald-500' : 'bg-red-500'}`} aria-hidden />
        {status.label}
      </p>
      <dl className="mt-2 space-y-1 text-sm">
        {HOURS.map((line) => (
          <div key={line.days} className="flex gap-2">
            <Clock className="mt-0.5 size-4 shrink-0 opacity-60" aria-hidden />
            <dt>{line.days} :</dt>
            <dd className="font-semibold">{line.hours}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
