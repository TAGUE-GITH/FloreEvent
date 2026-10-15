import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, ChevronLeft, ChevronRight, MapPin, Phone } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage, planningApi } from '../../api/client'
import type { PlanningTask } from '../../api/types'
import { Alert, Badge, Button, Card, Loader, PageHeader } from '../../components/ui'
import { todayIso } from '../../lib/dates'

/** Jour decale de n jours (dates « AAAA-MM-JJ », calcul a midi UTC pour eviter les changements d'heure). */
function addDays(day: string, n: number) {
  const date = new Date(`${day}T12:00:00Z`)
  date.setUTCDate(date.getUTCDate() + n)
  return date.toISOString().slice(0, 10)
}

/** Lundi de la semaine du jour donne. */
function monday(day: string) {
  const weekday = (new Date(`${day}T12:00:00Z`).getUTCDay() + 6) % 7
  return addDays(day, -weekday)
}

const dayTitle = (day: string) =>
  new Date(`${day}T12:00:00Z`).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })

const DEPARTURE = ['LIVRAISON', 'RETRAIT']

// US-14 : livraisons, retraits, reprises et retours de la semaine, avec le materiel a charger
export default function PlanningPage() {
  const today = todayIso()
  const [start, setStart] = useState(() => monday(today))
  const end = addDays(start, 6)
  const planning = useQuery({ queryKey: ['planning', start], queryFn: () => planningApi.list(start, end) })
  const days = Array.from({ length: 7 }, (_, i) => addDays(start, i))

  return (
    <>
      <PageHeader
        title="Planning"
        subtitle="Départs et retours du matériel, jour par jour."
        actions={
          <>
            <Button variant="secondary" aria-label="Semaine précédente" onClick={() => setStart(addDays(start, -7))}>
              <ChevronLeft className="size-4" aria-hidden />
            </Button>
            <Button variant="secondary" onClick={() => setStart(monday(today))}>
              Cette semaine
            </Button>
            <Button variant="secondary" aria-label="Semaine suivante" onClick={() => setStart(addDays(start, 7))}>
              <ChevronRight className="size-4" aria-hidden />
            </Button>
          </>
        }
      />
      {planning.isPending && <Loader />}
      <Alert>{planning.isError ? errorMessage(planning.error) : ''}</Alert>
      {planning.data && (
        <div className="space-y-6">
          {days.map((day) => {
            const tasks = planning.data.filter((t) => t.date === day)
            return (
              <section key={day} aria-label={dayTitle(day)}>
                <h2 className={`mb-2 font-bold first-letter:uppercase ${day === today ? 'text-brand-800' : ''}`}>
                  {dayTitle(day)}
                  {day === today && <span className="ml-2 text-sm font-semibold">· aujourd’hui</span>}
                </h2>
                {tasks.length === 0 ? (
                  <p className="text-sm text-slate-500">Rien de prévu.</p>
                ) : (
                  <div className="grid gap-3 md:grid-cols-2">
                    {tasks.map((task) => (
                      <TaskCard key={`${task.bookingId}-${task.kind}`} task={task} />
                    ))}
                  </div>
                )}
              </section>
            )
          })}
        </div>
      )}
    </>
  )
}

function TaskCard({ task }: { task: PlanningTask }) {
  const departure = DEPARTURE.includes(task.kind)
  return (
    <Card className={`space-y-2 text-sm ${task.done ? 'opacity-70' : ''}`}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Badge tone={departure ? 'brand' : 'amber'}>{task.kindLabel}</Badge>
        {task.done && (
          <span className="flex items-center gap-1 font-semibold text-emerald-700">
            <CheckCircle2 className="size-4" aria-hidden />
            Fait
          </span>
        )}
      </div>
      <p>
        <Link to={`/gestion/reservations/${task.bookingId}`} className="font-bold text-brand-700">
          {task.reference}
        </Link>{' '}
        · {task.customer}
      </p>
      <a href={`tel:${task.phone}`} className="flex items-center gap-2 font-semibold text-brand-700">
        <Phone className="size-4" aria-hidden />
        {task.phone}
      </a>
      {task.address && (
        <p className="flex gap-2 text-slate-700">
          <MapPin className="mt-0.5 size-4 shrink-0 text-slate-400" aria-hidden />
          <span>
            {task.address}
            {task.installation && departure ? ' · avec installation' : ''}
          </span>
        </p>
      )}
      <ul className="border-t border-stone-100 pt-2 text-slate-700">
        {task.lines.map((line) => (
          <li key={line.itemId}>
            {line.quantity} × {line.itemName}
          </li>
        ))}
      </ul>
    </Card>
  )
}
