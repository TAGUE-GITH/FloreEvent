import { addItem, emptySelection, itemCount, readStoredSelection, rentalDays, setQuantity, toRequest } from './selection'

const barnum = { id: 1, name: 'Barnum 3 x 6 m', dayPriceCents: 15000, setupPriceCents: 8000, imageUrl: '/api/images/a' }
const chaise = { id: 2, name: 'Chaise Napoléon', dayPriceCents: 350, setupPriceCents: 0 }

describe('selection de materiel (le devis en cours)', () => {
  it('ajoute des articles et regroupe les quantites du meme article', () => {
    let selection = addItem(emptySelection(), barnum, 2)
    selection = addItem(selection, chaise, 50)
    selection = addItem(selection, barnum, 1)

    expect(selection.lines.map((line) => [line.name, line.quantity])).toEqual([
      ['Barnum 3 x 6 m', 3],
      ['Chaise Napoléon', 50],
    ])
    expect(itemCount(selection)).toBe(53)
  })

  it('change une quantite et retire la ligne a zero', () => {
    const selection = setQuantity(addItem(emptySelection(), chaise, 50), 2, 80)
    expect(selection.lines[0].quantity).toBe(80)
    expect(setQuantity(selection, 2, 0).lines).toEqual([])
  })

  it('compte les jours de location, dates incluses', () => {
    expect(rentalDays('2026-11-14', '2026-11-14')).toBe(1)
    expect(rentalDays('2026-11-14', '2026-11-16')).toBe(3)
    expect(rentalDays('2026-11-16', '2026-11-14')).toBeNull()
    expect(rentalDays('', '2026-11-14')).toBeNull()
  })

  it('prepare la demande envoyee au serveur', () => {
    const selection = { ...addItem(emptySelection(), barnum, 2), startDate: '2026-11-14', endDate: '2026-11-15', deliveryMode: 'RETRAIT' as const, address: 'ignorée', installation: true, note: 'Mariage' }
    expect(toRequest(selection)).toEqual({
      startDate: '2026-11-14',
      endDate: '2026-11-15',
      deliveryMode: 'RETRAIT',
      address: '',
      installation: false,
      note: 'Mariage',
      lines: [{ itemId: 1, quantity: 2 }],
    })
  })

  it('relit une selection gardee dans le navigateur, et ignore un contenu abime', () => {
    const selection = addItem(emptySelection(), chaise, 10)
    expect(readStoredSelection(JSON.stringify(selection))).toEqual(selection)
    expect(readStoredSelection('{pas du json')).toEqual(emptySelection())
    expect(readStoredSelection(null)).toEqual(emptySelection())
    expect(readStoredSelection('{"lines": [{"itemId": "x"}]}')).toEqual(emptySelection())
  })
})
