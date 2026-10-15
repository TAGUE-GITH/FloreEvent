import { useQuery } from '@tanstack/react-query'
import { ArrowRight, CalendarCheck, Check, ClipboardList, CreditCard, Flower2, MapPin, Package, PartyPopper, Phone, ShieldCheck, Sparkles, Truck } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { catalogApi, imageUrl } from '../../api/client'
import { buttonClasses } from '../../components/buttonClasses'
import { COMPANY, DELIVERY_ZONES, OPENING_HOURS } from '../../lib/company'
import { formatEuros } from '../../lib/format'
import { Picture } from '../../components/Picture'
import { PHOTOS } from '../../lib/photos'
import type { Photo } from '../../lib/photos'
import { DatesBar } from '../quote/DatesBar'

const OCCASIONS: { title: string; text: string; points: string[]; photo: Photo }[] = [
  {
    title: 'Mariages',
    text: 'De la cérémonie au dîner : tentes, mobilier élégant, arche fleurie et art de la table.',
    points: ['Tente de réception jusqu’à 80 invités', 'Arche et centres de table fleuris', 'Mise en place de la salle'],
    photo: PHOTOS.wedding,
  },
  {
    title: 'Anniversaires et fêtes',
    text: 'Barnums, guirlandes lumineuses et sonorisation pour faire la fête chez vous ou en salle.',
    points: ['Barnums pour le jardin', 'Lumières et pack DJ', 'Retrait gratuit à Melun'],
    photo: PHOTOS.birthday,
  },
  {
    title: 'Séminaires et entreprises',
    text: 'Tables, sonorisation et accueil pour vos réunions, salons et soirées d’entreprise.',
    points: ['Devis et facture au nom de la société', 'Sonorisation avec micros', 'Livraison et installation'],
    photo: PHOTOS.seminar,
  },
]

const STEPS: { title: string; text: string; Icon: LucideIcon }[] = [
  { title: 'Composez votre devis', text: 'Choisissez vos dates et votre matériel : le prix se calcule en direct, livraison comprise.', Icon: ClipboardList },
  { title: 'Nous le validons', text: 'Notre équipe vérifie tout et vous envoie votre devis définitif sous 24 h ouvrées.', Icon: CalendarCheck },
  { title: 'Payez l’acompte en ligne', text: '30 % par carte sur une page sécurisée : votre matériel est réservé.', Icon: CreditCard },
  { title: 'Profitez de votre fête', text: 'Nous livrons, installons puis reprenons tout après l’événement.', Icon: PartyPopper },
]

const GALLERY: { photo: Photo; caption: string }[] = [
  { photo: PHOTOS.tent, caption: 'Réception sous tente' },
  { photo: PHOTOS.tables, caption: 'Tables fleuries' },
  { photo: PHOTOS.chairs, caption: 'Cérémonie en plein air' },
  { photo: PHOTOS.ballroom, caption: 'Dîner en salle' },
  { photo: PHOTOS.party, caption: 'Soirée guinguette' },
  { photo: PHOTOS.dj, caption: 'Piste de danse' },
]

const TRUST: { label: string; Icon: LucideIcon }[] = [
  { label: 'Devis immédiat en ligne', Icon: Sparkles },
  { label: 'Matériel contrôlé et nettoyé', Icon: ShieldCheck },
  { label: 'Livraison jusqu’à 50 km', Icon: Truck },
]

// Accueil du site public : la vitrine de FloreEvent
export default function HomePage() {
  return (
    <>
      <section className="relative isolate overflow-hidden bg-stone-950 text-white">
        <Picture photo={PHOTOS.hero} ratio={16 / 9} sizes="100vw" eager className="absolute inset-0 -z-10 size-full" />
        <div className="absolute inset-0 -z-10 bg-stone-950/70 sm:bg-transparent sm:bg-gradient-to-r sm:from-stone-950/90 sm:via-stone-950/65 sm:to-stone-950/20" aria-hidden />
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-24">
          <p className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-sm font-semibold ring-1 ring-white/20 backdrop-blur">
            <MapPin className="size-4" aria-hidden />
            {COMPANY.city} · Seine-et-Marne
          </p>
          <h1 className="mt-5 max-w-2xl text-4xl leading-tight font-extrabold tracking-tight sm:text-6xl">Votre événement, clé en main</h1>
          <p className="mt-5 max-w-xl text-lg text-stone-200">
            Location de tentes, mobilier, art de la table, lumière et sonorisation, avec décoration florale sur mesure. Livré, installé et repris.
          </p>
          <div className="mt-8 max-w-3xl text-ink">
            <DatesBar />
          </div>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/catalogue" className={buttonClasses('primary', 'px-6 py-3 text-base')}>
              Voir le matériel disponible
              <ArrowRight className="size-4" aria-hidden />
            </Link>
            <Link to="/contact" className="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-3 text-base font-semibold text-ink hover:bg-stone-100">
              <Phone className="size-4" aria-hidden />
              Nous contacter
            </Link>
          </div>
          <ul className="mt-10 flex flex-wrap gap-x-6 gap-y-2 text-sm text-stone-200">
            {TRUST.map(({ label, Icon }) => (
              <li key={label} className="flex items-center gap-2">
                <Icon className="size-4 text-brand-300" aria-hidden />
                {label}
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-20">
        <h2 className="text-center text-3xl font-extrabold tracking-tight sm:text-4xl">Pour tous vos événements</h2>
        <p className="mx-auto mt-2 max-w-2xl text-center text-slate-600">Particuliers, associations et entreprises : nous équipons votre fête, petite ou grande.</p>
        <ul className="mt-10 grid gap-6 md:grid-cols-3">
          {OCCASIONS.map(({ title, text, points, photo }) => (
            <li key={title} className="group flex flex-col overflow-hidden rounded-2xl bg-white ring-1 ring-stone-200 transition hover:shadow-lg">
              <div className="overflow-hidden">
                <Picture photo={photo} ratio={3 / 2} sizes="(min-width: 768px) 33vw, 100vw" className="aspect-[3/2] w-full transition duration-500 group-hover:scale-105" />
              </div>
              <div className="flex flex-1 flex-col p-6">
                <h3 className="text-xl font-bold">{title}</h3>
                <p className="mt-1 text-slate-600">{text}</p>
                <ul className="mt-4 space-y-2 text-sm text-slate-700">
                  {points.map((point) => (
                    <li key={point} className="flex gap-2">
                      <Check className="mt-0.5 size-4 shrink-0 text-brand-600" aria-hidden />
                      {point}
                    </li>
                  ))}
                </ul>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <CatalogPreview />
      <Prestations />

      <section className="bg-stone-100">
        <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-20">
          <Picture photo={PHOTOS.dinner} ratio={4 / 3} sizes="(min-width: 1024px) 50vw, 100vw" className="aspect-[4/3] w-full rounded-3xl shadow-xl" />
          <div>
            <h2 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Comment ça marche ?</h2>
            <ol className="mt-8 space-y-5">
              {STEPS.map(({ title, text, Icon }, index) => (
                <li key={title} className="flex gap-4">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-brand-700 text-white" aria-label={`Étape ${index + 1}`}>
                    <Icon className="size-5" aria-hidden />
                  </span>
                  <div>
                    <h3 className="font-bold">{title}</h3>
                    <p className="mt-0.5 text-slate-600">{text}</p>
                  </div>
                </li>
              ))}
            </ol>
            <Link to="/catalogue" className={buttonClasses('primary', 'mt-8 px-6 py-3 text-base')}>
              Composer mon devis
              <ArrowRight className="size-4" aria-hidden />
            </Link>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-20">
        <h2 className="text-center text-3xl font-extrabold tracking-tight sm:text-4xl">Nos réalisations</h2>
        <p className="mx-auto mt-2 max-w-2xl text-center text-slate-600">Quelques ambiances que nous aimons créer.</p>
        <ul className="mt-10 grid grid-cols-2 gap-4 lg:grid-cols-3">
          {GALLERY.map(({ photo, caption }) => (
            <li key={caption} className="relative overflow-hidden rounded-2xl">
              <Picture photo={photo} ratio={4 / 3} sizes="(min-width: 1024px) 33vw, 50vw" className="aspect-[4/3] w-full" />
              <p className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-stone-950/85 to-transparent p-3 pt-10 text-sm font-semibold text-white">{caption}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="mx-auto grid max-w-6xl gap-8 px-4 pb-16 sm:px-6 lg:grid-cols-2 lg:pb-20">
        <div className="rounded-3xl bg-white p-6 ring-1 ring-stone-200 sm:p-8">
          <h2 className="flex items-center gap-2 text-2xl font-extrabold tracking-tight">
            <Truck className="size-6 text-brand-700" aria-hidden />
            Livraison depuis {COMPANY.city}
          </h2>
          <p className="mt-2 text-slate-600">Livraison et reprise comprises, calculées automatiquement dans votre devis. Retrait gratuit à notre entrepôt.</p>
          <dl className="mt-5 divide-y divide-stone-100">
            {DELIVERY_ZONES.map((zone) => (
              <div key={zone.upTo} className="flex justify-between py-2">
                <dt className="text-slate-700">{zone.upTo}</dt>
                <dd className="font-semibold">{zone.price}</dd>
              </div>
            ))}
          </dl>
        </div>
        <div className="rounded-3xl bg-brand-900 p-6 text-white sm:p-8">
          <h2 className="text-2xl font-extrabold tracking-tight">Venez nous voir</h2>
          <p className="mt-2 flex gap-2 text-brand-50">
            <MapPin className="mt-1 size-5 shrink-0 text-brand-300" aria-hidden />
            {COMPANY.address}
          </p>
          <dl className="mt-5 space-y-1.5 text-sm">
            {OPENING_HOURS.map((h) => (
              <div key={h.days} className="flex flex-wrap justify-between gap-x-4">
                <dt className="font-semibold">{h.days}</dt>
                <dd className="text-brand-50">{h.hours}</dd>
              </div>
            ))}
          </dl>
          <div className="mt-6 flex flex-wrap gap-3">
            <a href={`tel:${COMPANY.phone.replace(/\s/g, '')}`} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-ink hover:bg-stone-100">
              <Phone className="size-4" aria-hidden />
              {COMPANY.phone}
            </a>
            <Link to="/contact" className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold ring-1 ring-white/40 hover:bg-white/10">
              Nous écrire
            </Link>
          </div>
        </div>
      </section>
    </>
  )
}

/** Quelques articles du catalogue (rien ne s'affiche tant que le catalogue est vide). */
function CatalogPreview() {
  const { data } = useQuery({ queryKey: ['home-items'], queryFn: () => catalogApi.items() })
  const items = data?.filter((item) => item.imageUrl).slice(0, 8) ?? []
  if (items.length === 0) return null
  return (
    <section className="mx-auto max-w-6xl px-4 pb-16 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight">Notre matériel</h2>
          <p className="mt-1 text-slate-600">Prix par jour TTC ; chaque jour supplémentaire à moitié prix.</p>
        </div>
        <Link to="/catalogue" className="font-semibold text-brand-700">
          Tout le catalogue →
        </Link>
      </div>
      <ul className="mt-8 grid grid-cols-2 gap-4 sm:gap-6 md:grid-cols-4">
        {items.map((item) => (
          <li key={item.id}>
            <Link to={`/catalogue/${item.id}`} className="group block overflow-hidden rounded-2xl bg-white ring-1 ring-stone-200 transition hover:shadow-lg">
              <div className="overflow-hidden">
                <img src={imageUrl(item.imageUrl)} alt={item.name} loading="lazy" className="aspect-[4/3] w-full bg-stone-200 object-cover transition duration-500 group-hover:scale-105" />
              </div>
              <div className="p-3">
                <p className="text-xs font-semibold text-brand-700">{item.category.name}</p>
                <p className="mt-0.5 line-clamp-2 font-semibold">{item.name}</p>
                <p className="mt-1 text-sm text-slate-600">
                  <span className="font-bold text-ink">{formatEuros(item.dayPriceCents)}</span> / jour
                </p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** Prestations sur mesure (rien ne s'affiche tant qu'aucune n'est publiee). */
function Prestations() {
  const { data } = useQuery({ queryKey: ['public-prestations'], queryFn: catalogApi.prestations })
  if (!data?.length) return null
  return (
    <section className="bg-brand-900 text-white">
      <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-20">
        <div>
          <p className="flex items-center gap-2 text-sm font-semibold tracking-wider text-brand-200 uppercase">
            <Flower2 className="size-4" aria-hidden />
            Prestations
          </p>
          <h2 className="mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">Nous nous occupons de tout</h2>
          <ul className="mt-6 space-y-4">
            {data.map((p) => (
              <li key={p.id} className="flex gap-3">
                <Package className="mt-1 size-5 shrink-0 text-brand-300" aria-hidden />
                <div>
                  <p className="font-bold">
                    {p.name} <span className="font-normal text-brand-100">· à partir de {formatEuros(p.priceCents)}</span>
                  </p>
                  {p.description && <p className="text-brand-50">{p.description}</p>}
                </div>
              </li>
            ))}
          </ul>
          <p className="mt-6 text-sm text-brand-100">Ajoutez-les à votre devis en un clic, nous adaptons le prix à votre projet.</p>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Picture photo={PHOTOS.florist} ratio={3 / 4} sizes="(min-width: 1024px) 25vw, 50vw" className="aspect-[3/4] w-full rounded-2xl" />
          <Picture photo={PHOTOS.lights} ratio={3 / 4} sizes="(min-width: 1024px) 25vw, 50vw" className="mt-10 aspect-[3/4] w-full rounded-2xl" />
        </div>
      </div>
    </section>
  )
}
