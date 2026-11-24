package com.model;

// one row of the borrowing history: who took which book, how many copies, and when
public class Borrowing {
	private long id;
	private long userId;
	private String userName;
	private long bookId;
	private String bookTitle;
	private int quantity;
	private String borrowedAt;
	private String returnedAt; // null while the book is still out

	public Borrowing(long id, long userId, String userName, long bookId, String bookTitle, int quantity,
			String borrowedAt, String returnedAt) {
		this.id = id;
		this.userId = userId;
		this.userName = userName;
		this.bookId = bookId;
		this.bookTitle = bookTitle;
		this.quantity = quantity;
		this.borrowedAt = borrowedAt;
		this.returnedAt = returnedAt;
	}

	public long getId() { return id; }
	public long getUserId() { return userId; }
	public String getUserName() { return userName; }
	public long getBookId() { return bookId; }
	public String getBookTitle() { return bookTitle; }
	public int getQuantity() { return quantity; }
	public String getBorrowedAt() { return borrowedAt; }
	public String getReturnedAt() { return returnedAt; }
}
