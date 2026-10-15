import { Link } from 'react-router-dom'
import { LegalLayout } from './LegalLayout'
import type { LegalSection } from './LegalLayout'

// Conditions generales de location de FloreEvent (regles du cahier des charges, a faire valider par l'entreprise)
const SECTIONS: LegalSection[] = [
  {
    id: 'objet',
    title: 'Objet et acceptation',
    content: (
      <>
        <p>
          Les présentes conditions s’appliquent à la location de matériel événementiel (tentes, mobilier, vaisselle, sonorisation, décoration…)
          et aux services associés (livraison, installation) proposés par FloreEvent, à Melun (Seine-et-Marne).
        </p>
        <p>
          En créant un compte et en envoyant une demande de devis, le client déclare les avoir lues et acceptées ; la version acceptée et la date
          sont enregistrées. Les conditions applicables sont celles en vigueur à la date de la demande.
        </p>
      </>
    ),
  },
  {
    id: 'devis',
    title: 'Demande de devis et réservation',
    content: (
      <>
        <p>
          Le client choisit ses dates, son matériel et le retrait ou la livraison ; le prix est calculé en ligne. L’envoi d’une demande est gratuit
          et sans engagement : il ne bloque pas le matériel.
        </p>
        <p>
          FloreEvent confirme la disponibilité et envoie un devis, valable 7 jours. La réservation est ferme lorsque le client accepte le devis et
          paie l’acompte : le matériel lui est alors réservé pour ses dates.
        </p>
      </>
    ),
  },
  {
    id: 'prix',
    title: 'Prix',
    content: (
      <>
        <p>Les prix sont indiqués en euros, toutes taxes comprises (TVA 20 %). Le prix appliqué est celui du devis accepté.</p>
        <ul>
          <li>Location : tarif du premier jour, puis 50 % de ce tarif pour chaque jour supplémentaire.</li>
          <li>Retrait à l’entrepôt de Melun : gratuit. Livraison et reprise : selon la distance (jusqu’à 50 km, au-delà sur devis).</li>
          <li>Installation et démontage : forfait par article lorsqu’ils sont proposés.</li>
        </ul>
      </>
    ),
  },
  {
    id: 'paiement',
    title: 'Acompte, solde et caution',
    content: (
      <ul>
        <li>Acompte de 30 % à la réservation, payé en ligne par carte bancaire (paiement sécurisé : FloreEvent ne voit jamais les numéros de carte).</li>
        <li>Solde au plus tard à la livraison ou au retrait.</li>
        <li>Caution égale à 30 % de la valeur du matériel (150 € minimum), déposée à la livraison ou au retrait et rendue après le contrôle du retour.</li>
      </ul>
    ),
  },
  {
    id: 'annulation',
    title: 'Annulation',
    content: (
      <>
        <p>Une demande ou un devis non accepté peut être annulé à tout moment depuis l’espace client. Pour une réservation confirmée :</p>
        <ul>
          <li>plus de 30 jours avant le début de la location : l’acompte est remboursé ;</li>
          <li>de 30 à 8 jours avant : l’acompte est conservé ;</li>
          <li>moins de 8 jours avant : 50 % du montant total reste dû.</li>
        </ul>
        <p>Ces conditions ne privent pas le client consommateur des droits que lui accorde le Code de la consommation.</p>
      </>
    ),
  },
  {
    id: 'usage',
    title: 'Utilisation, retour, casse et perte',
    content: (
      <>
        <p>
          Le client utilise le matériel avec soin, conformément à sa destination, et le rend dans l’état où il l’a reçu. Un état des lieux est fait à
          la livraison et au retour.
        </p>
        <p>
          Le matériel abîmé ou manquant est facturé à sa valeur de remplacement, indiquée dans le devis, et retenu sur la caution. Un retard de retour
          peut être facturé au tarif d’un jour supplémentaire par jour de retard.
        </p>
      </>
    ),
  },
  {
    id: 'livraison',
    title: 'Livraison et installation',
    content: (
      <p>
        Le client garantit un accès praticable au lieu de l’événement et la présence d’une personne pour la livraison et la reprise. Les créneaux
        sont convenus avec lui. Les installations sont réalisées par l’équipe de FloreEvent.
      </p>
    ),
  },
  {
    id: 'donnees',
    title: 'Données personnelles',
    content: (
      <p>
        Les informations recueillies servent à gérer les demandes et les réservations. Leur utilisation et les droits du client sont décrits dans
        la{' '}
        <Link to="/confidentialite" className="font-semibold text-brand-700">
          politique de confidentialité
        </Link>
        .
      </p>
    ),
  },
  {
    id: 'litiges',
    title: 'Réclamations et droit applicable',
    content: (
      <p>
        Les présentes conditions sont soumises au droit français. Toute réclamation peut être adressée à FloreEvent, qui recherche d’abord une
        solution amiable. Le client consommateur peut aussi recourir gratuitement à un médiateur de la consommation. À défaut d’accord, le litige
        est porté devant les juridictions compétentes.
      </p>
    ),
  },
]

export default function TermsPage() {
  return <LegalLayout title="Conditions générales de location" intro={<p>Conditions de location du matériel et des services de FloreEvent.</p>} sections={SECTIONS} />
}
