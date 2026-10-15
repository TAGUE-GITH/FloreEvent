package com.floreevent.booking;

import com.floreevent.catalog.Item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Article reserve : nom et prix figes au moment de la demande (R3). */
@Entity
@Table(name = "booking_line")
public class BookingLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id")
	private Item item;

	@Column(name = "item_name", nullable = false)
	private String itemName;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "day_price_cents", nullable = false)
	private int dayPriceCents;

	@Column(name = "setup_price_cents", nullable = false)
	private int setupPriceCents;

	@Column(name = "replacement_value_cents", nullable = false)
	private int replacementValueCents;

	@Column(name = "line_total_cents", nullable = false)
	private int lineTotalCents;

	protected BookingLine() {
	}

	BookingLine(Booking booking, Item item, int quantity, int lineTotalCents) {
		this.booking = booking;
		this.item = item;
		this.itemName = item.getName();
		this.quantity = quantity;
		this.dayPriceCents = item.getDayPriceCents();
		this.setupPriceCents = item.getSetupPriceCents();
		this.replacementValueCents = item.getReplacementValueCents();
		this.lineTotalCents = lineTotalCents;
	}

	public Item getItem() {
		return item;
	}

	public String getItemName() {
		return itemName;
	}

	public int getQuantity() {
		return quantity;
	}

	public int getDayPriceCents() {
		return dayPriceCents;
	}

	public int getSetupPriceCents() {
		return setupPriceCents;
	}

	public int getReplacementValueCents() {
		return replacementValueCents;
	}

	public int getLineTotalCents() {
		return lineTotalCents;
	}
}
