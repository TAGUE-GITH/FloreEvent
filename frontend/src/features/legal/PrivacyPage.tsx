import { LegalLayout } from './LegalLayout'
import type { LegalSection } from './LegalLayout'

// Politique de confidentialite (RGPD) : quelles donnees, pourquoi, combien de temps, droits du client
const SECTIONS: LegalSection[] = [
  {
    id: 'responsable',
    title: 'Responsable du traitement',
    content: <p>FloreEvent, entreprise de location événementielle à Melun (Seine-et-Marne), est responsable des données recueillies sur ce site.</p>,
  },
  {
    id: 'donnees',
    title: 'Données recueillies',
    content: (
      <ul>
        <li>Compte : nom, prénom, email, téléphone, société (facultatif), mot de passe (enregistré sous une forme chiffrée).</li>
        <li>Demandes et réservations : dates, matériel, adresse de l’événement, montants, paiements.</li>
        <li>Paiement de l’acompte : traité par notre prestataire de paiement sécurisé ; FloreEvent ne reçoit jamais les numéros de carte.</li>
      </ul>
    ),
  },
  {
    id: 'finalites',
    title: 'Finalités et bases légales',
    content: (
      <ul>
        <li>Gérer les devis, les réservations, les livraisons et la facturation : exécution du contrat.</li>
        <li>Tenir la comptabilité : obligation légale.</li>
        <li>Assurer la sécurité du site et des comptes : intérêt légitime.</li>
      </ul>
    ),
  },
  {
    id: 'destinataires',
    title: 'Destinataires',
    content: (
      <>
        <p>
          Seule l’équipe de FloreEvent y a accès, selon son rôle. Les données ne sont ni vendues ni utilisées pour de la publicité. Des prestataires
          techniques les traitent uniquement pour faire fonctionner le service : hébergement (Render, Neon, Vercel), envoi des emails (Brevo),
          paiement (Stripe) et vérification des adresses (service public de géocodage de l’IGN).
        </p>
      </>
    ),
  },
  {
    id: 'duree',
    title: 'Durées de conservation',
    content: (
      <ul>
        <li>Compte et historique : pendant la relation commerciale, puis 3 ans après le dernier contact.</li>
        <li>Factures et pièces comptables : 10 ans, comme l’exige la loi.</li>
      </ul>
    ),
  },
  {
    id: 'stockage',
    title: 'Cookies et stockage dans le navigateur',
    content: (
      <p>
        Le site n’utilise ni cookie publicitaire ni outil de mesure d’audience. Il garde seulement dans le navigateur la connexion et la sélection en
        cours (le devis), pour qu’elles ne soient pas perdues d’une page à l’autre.
      </p>
    ),
  },
  {
    id: 'droits',
    title: 'Vos droits',
    content: (
      <p>
        Conformément au RGPD et à la loi Informatique et Libertés, vous pouvez accéder à vos données, les faire rectifier ou effacer, vous opposer à
        leur traitement ou en demander la portabilité, en écrivant à FloreEvent. Vous pouvez aussi adresser une réclamation à la CNIL (www.cnil.fr).
      </p>
    ),
  },
]

export default function PrivacyPage() {
  return <LegalLayout title="Politique de confidentialité" intro={<p>Comment FloreEvent protège les données de ses clients.</p>} sections={SECTIONS} />
}
