import { ArrowRight, Calculator, ShieldCheck, Truck } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { buttonClasses } from '../../components/buttonClasses'
import { DatesBar } from '../quote/DatesBar'

const POINTS: { title: string; text: string; Icon: LucideIcon }[] = [
  { title: 'Devis immédiat', text: 'Choisissez vos dates et votre matériel : le prix se calcule en direct, livraison comprise.', Icon: Calculator },
  { title: 'Livré et installé', text: 'Livraison, installation et reprise autour de Melun, ou retrait gratuit à notre entrepôt.', Icon: Truck },
  { title: 'Matériel contrôlé', text: 'Chaque article est vérifié et nettoyé entre deux événements.', Icon: ShieldCheck },
]

// Accueil du site public (la vitrine complete avec photos arrive au sprint 6)
export default function HomePage() {
  return (
    <>
      <section className="bg-gradient-to-br from-brand-900 via-brand-800 to-brand-700 text-white">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-20">
          <p className="text-sm font-semibold tracking-wider text-brand-200 uppercase">Melun · Seine-et-Marne</p>
          <h1 className="mt-3 max-w-2xl text-4xl leading-tight font-extrabold tracking-tight sm:text-5xl">Location de matériel pour vos événements</h1>
          <p className="mt-4 max-w-xl text-lg text-brand-50">Tentes, mobilier, vaisselle, sonorisation et décoration florale pour vos mariages, anniversaires et séminaires.</p>
          <div className="mt-8 max-w-3xl text-ink">
            <DatesBar />
          </div>
          <Link to="/catalogue" className={buttonClasses('secondary', 'mt-6 px-6 py-3 text-base')}>
            Voir le matériel disponible
            <ArrowRight className="size-4" aria-hidden />
          </Link>
        </div>
      </section>
      <section className="mx-auto max-w-6xl px-4 py-14 sm:px-6">
        <ul className="grid gap-6 md:grid-cols-3">
          {POINTS.map(({ title, text, Icon }) => (
            <li key={title} className="rounded-2xl bg-white p-6 ring-1 ring-stone-200">
              <span className="flex size-11 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
                <Icon className="size-5" aria-hidden />
              </span>
              <h2 className="mt-4 text-lg font-bold">{title}</h2>
              <p className="mt-1 text-slate-600">{text}</p>
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}
