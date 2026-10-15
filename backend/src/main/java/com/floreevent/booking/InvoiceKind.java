package com.floreevent.booking;

public enum InvoiceKind {
	ACOMPTE("Facture d'acompte"),
	SOLDE("Facture de solde"),
	AVOIR("Avoir");

	private final String label;

	InvoiceKind(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
