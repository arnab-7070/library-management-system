package com.model;

// Book class for creating the books
public class Book {
	// private properties of the book (Gson turns these into JSON fields)
	private long id;
	private String title;
	private String author;
	private double price;
	private int quantity;

	public Book(long id, String title, String author, double price, int quantity) {
		this.id = id;
		this.title = title;
		this.author = author;
		this.price = price;
		this.quantity = quantity;
	}

	public long getId() { return id; }
	public String getTitle() { return title; }
	public String getAuthor() { return author; }
	public double getPrice() { return price; }
	public int getQuantity() { return quantity; }

	@Override
	public String toString() {
		return "Title : " + title + ", Author : " + author + ", Price : " + price + ", Quantity : " + quantity;
	}
}
