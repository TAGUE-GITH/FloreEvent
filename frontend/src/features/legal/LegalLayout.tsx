import type { ReactNode } from 'react'
import { TERMS_DATE } from '../../lib/legal'

export type LegalSection = { id: string; title: string; content: ReactNode }

/** Mise en page commune des textes juridiques : sommaire, sections numerotees, date de version. */
export function LegalLayout({ title, intro, sections }: { title: string; intro: ReactNode; sections: LegalSection[] }) {
  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold tracking-tight">{title}</h1>
      <p className="mt-1 text-sm text-slate-500">Version du {TERMS_DATE}</p>
      <div className="mt-5 text-slate-700">{intro}</div>

      <nav aria-label="Sommaire" className="mt-6 rounded-2xl bg-white p-5 ring-1 ring-stone-200">
        <p className="text-sm font-bold">Sommaire</p>
        <ol className="mt-2 grid gap-1 text-sm sm:grid-cols-2">
          {sections.map((section, index) => (
            <li key={section.id}>
              <a href={`#${section.id}`} className="text-brand-700 hover:underline">
                {index + 1}. {section.title}
              </a>
            </li>
          ))}
        </ol>
      </nav>

      <div className="mt-8 space-y-8">
        {sections.map((section, index) => (
          <section key={section.id} id={section.id} className="scroll-mt-24">
            <h2 className="text-xl font-bold">
              {index + 1}. {section.title}
            </h2>
            <div className="mt-2 space-y-2 leading-relaxed text-slate-700 [&_li]:ml-5 [&_li]:list-disc">{section.content}</div>
          </section>
        ))}
      </div>

      <p className="mt-10 border-t border-stone-200 pt-5 text-sm text-slate-500">
        Une question sur ce texte ? Écrivez-nous à l’adresse indiquée ci-dessus ou depuis votre espace client.
      </p>
    </div>
  )
}
