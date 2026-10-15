import { COMPANY } from '../../lib/company'
import { LegalLayout } from './LegalLayout'
import type { LegalSection } from './LegalLayout'

// Mentions legales (LCEN) : editeur, hebergeurs, propriete intellectuelle. A completer avec les informations officielles de FloreEvent.
const SECTIONS: LegalSection[] = [
  {
    id: 'editeur',
    title: 'Éditeur du site',
    content: (
      <ul>
        <li>{COMPANY.name}, location de matériel et services événementiels.</li>
        <li>Adresse : {COMPANY.address}.</li>
        <li>
          Téléphone : {COMPANY.phone} · Email : {COMPANY.email}
        </li>
        <li>Forme juridique, capital, RCS, SIRET et numéro de TVA : à compléter par FloreEvent.</li>
        <li>Directeur de la publication : le gérant de FloreEvent.</li>
      </ul>
    ),
  },
  {
    id: 'hebergement',
    title: 'Hébergement',
    content: (
      <ul>
        <li>Site : Vercel Inc., 440 N Barranca Ave #4133, Covina, CA 91723, États-Unis.</li>
        <li>Serveur de l’application : Render Services, Inc., San Francisco, États-Unis (serveurs en Europe, Francfort).</li>
        <li>Base de données : Neon Inc. (serveurs en Europe, Francfort).</li>
      </ul>
    ),
  },
  {
    id: 'propriete',
    title: 'Propriété intellectuelle',
    content: (
      <p>
        Les textes et le logo du site appartiennent à {COMPANY.name}. Les photos d’illustration proviennent d’Unsplash (licence Unsplash, usage libre) et seront
        remplacées par les photos des événements de {COMPANY.name}.
      </p>
    ),
  },
  {
    id: 'donnees',
    title: 'Données personnelles et cookies',
    content: (
      <p>
        Le site n’utilise pas de cookies publicitaires ni de mesure d’audience. Il garde seulement votre connexion et votre devis en cours dans votre navigateur.
        Le traitement de vos données est détaillé dans la politique de confidentialité.
      </p>
    ),
  },
]

export default function LegalNoticePage() {
  return <LegalLayout title="Mentions légales" intro={<p>Informations sur l’éditeur et l’hébergement du site {COMPANY.name}.</p>} sections={SECTIONS} />
}
