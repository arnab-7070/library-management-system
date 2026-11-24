// All the screen logic. Every call to the backend goes through Api (see api.js).
const $ = (sel) => document.querySelector(sel);
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const rupees = (n) => "₹" + Number(n).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const when = (iso) => (iso ? new Date(iso).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "");
const spineColor = (text) => {
  let h = 0;
  for (const ch of text) h = (h * 31 + ch.charCodeAt(0)) % 360;
  return `hsl(${h} 35% 38%)`;
};
const table = (head, rows) =>
  `<div class="table-wrap"><table><thead><tr>${head}</tr></thead><tbody>${rows}</tbody></table></div>`;

let toastTimer;
function toast(msg, bad = false) {
  const t = $("#toast");
  t.textContent = msg;
  t.className = "show" + (bad ? " bad" : "");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (t.className = ""), 3500);
}
// run an action, show a toast if the server says no
async function attempt(fn) {
  try { return await fn(); } catch (e) { toast(e.message, true); }
}

// ---------------- sign in / first-time setup ----------------
let setupMode = false;

async function start() {
  if (Api.token) return showApp();
  try {
    setupMode = !(await Api.get("/api/auth/status")).hasLibrarian;
  } catch (e) {
    $("#auth").hidden = false;
    $("#auth-error").textContent = e.message;
    return;
  }
  $("#auth").hidden = false;
  $("#auth-intro").textContent = setupMode
    ? "Welcome. Create the librarian account to get started."
    : "Sign in to manage books, members and loans.";
  $("#auth-submit").textContent = setupMode ? "Create librarian account" : "Sign in";
  $("#auth-hint").hidden = !setupMode;
}

$("#auth-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  $("#auth-error").textContent = "";
  const creds = Object.fromEntries(new FormData(e.target));
  try {
    if (setupMode) await Api.post("/api/auth/register", creds);
    const { token } = await Api.post("/api/auth/login", creds);
    Api.setToken(token);
    $("#auth").hidden = true;
    showApp();
  } catch (err) {
    $("#auth-error").textContent = err.message;
  }
});

$("#signout").addEventListener("click", async () => {
  await attempt(() => Api.post("/api/auth/logout"));
  Api.setToken(null);
  location.reload();
});

// ---------------- navigation ----------------
function showApp() {
  $("#app").hidden = false;
  show("dashboard");
}
function show(view) {
  document.querySelectorAll("#nav button").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
  document.querySelectorAll(".content > section").forEach((s) => (s.hidden = s.id !== "view-" + view));
  ({ dashboard: loadDashboard, books: loadBooks, users: loadUsers, circulation: loadCirculation })[view]();
}
$("#nav").addEventListener("click", (e) => e.target.dataset.view && show(e.target.dataset.view));

// ---------------- dashboard ----------------
async function loadDashboard() {
  const [s, out] = await Promise.all([Api.get("/api/stats"), Api.get("/api/borrowings?status=active")]).catch((e) => (toast(e.message, true), []));
  if (!s) return;
  const items = [["Titles on the catalogue", s.titles], ["Copies on the shelves", s.copiesInStock], ["Copies out on loan", s.copiesOut], ["Registered members", s.members]];
  $("#ledger").innerHTML = items.map(([k, v]) => `<div><dt>${k}</dt><dd>${v}</dd></div>`).join("");
  $("#dash-out").innerHTML = out.length
    ? table("<th>Member</th><th>Book</th><th class='num'>Copies</th><th>Issued</th>",
        out.map((b) => `<tr><td>${esc(b.userName)}</td><td>${esc(b.bookTitle)}</td><td class="num">${b.quantity}</td><td>${when(b.borrowedAt)}</td></tr>`).join(""))
    : `<p class="empty">Nothing is out on loan right now.</p>`;
}

// ---------------- books ----------------
async function loadBooks() {
  const q = $("#book-search").value;
  const books = await attempt(() => Api.get("/api/books?q=" + encodeURIComponent(q)));
  if (!books) return;
  $("#book-list").innerHTML = books.length
    ? table("<th></th><th>Title</th><th>Author</th><th class='num'>Price</th><th class='num'>Copies</th><th>Status</th><th></th>",
        books.map((b) => `<tr>
          <td class="spine"><span style="background:${spineColor(b.title)}"></span></td>
          <td>${esc(b.title)}</td><td>${esc(b.author)}</td>
          <td class="num">${rupees(b.price)}</td><td class="num">${b.quantity}</td>
          <td>${b.quantity === 0 ? '<span class="stamp out">All out</span>' : b.quantity <= 2 ? '<span class="stamp low">Low stock</span>' : '<span class="stamp in">On shelf</span>'}</td>
          <td class="actions"><button class="danger" data-delete="${b.id}">Delete</button></td></tr>`).join(""))
    : `<p class="empty">${q ? "No books match that search." : "No books yet. Add your first book above."}</p>`;
}
$("#book-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.target;
  if (await attempt(() => Api.post("/api/books", Object.fromEntries(new FormData(form))))) {
    toast("Book added.");
    form.reset();
    form.quantity.value = 1;
    loadBooks();
  }
});
let searchTimer;
$("#book-search").addEventListener("input", () => { clearTimeout(searchTimer); searchTimer = setTimeout(loadBooks, 250); });
$("#book-list").addEventListener("click", async (e) => {
  const id = e.target.dataset.delete;
  if (id && confirm("Delete this book from the catalogue?")) {
    if (await attempt(() => Api.del("/api/books/" + id))) { toast("Book deleted."); loadBooks(); }
  }
});

// ---------------- members ----------------
async function loadUsers() {
  const users = await attempt(() => Api.get("/api/users"));
  if (!users) return;
  $("#user-list").innerHTML = users.length
    ? table("<th class='num'>ID</th><th>Name</th><th>Phone</th>",
        users.map((u) => `<tr><td class="num">${u.id}</td><td>${esc(u.name)}</td><td>${esc(u.phone)}</td></tr>`).join(""))
    : `<p class="empty">No members yet. Add the first member above.</p>`;
}
$("#user-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (await attempt(() => Api.post("/api/users", Object.fromEntries(new FormData(e.target))))) {
    toast("Member added.");
    e.target.reset();
    loadUsers();
  }
});

// ---------------- issue & return ----------------
let loanStatus = "active";
async function loadCirculation() {
  const [users, books] = await Promise.all([Api.get("/api/users"), Api.get("/api/books")]).catch((e) => (toast(e.message, true), []));
  if (!users) return;
  $("#issue-user").innerHTML = users.map((u) => `<option value="${u.id}">${esc(u.name)} (${esc(u.phone)})</option>`).join("") || "<option value=''>Add a member first</option>";
  const available = books.filter((b) => b.quantity > 0);
  $("#issue-book").innerHTML = available.map((b) => `<option value="${b.id}">${esc(b.title)}, ${esc(b.author)} (${b.quantity} left)</option>`).join("") || "<option value=''>No books in stock</option>";
  loadLoans();
}
async function loadLoans() {
  const loans = await attempt(() => Api.get("/api/borrowings?status=" + loanStatus));
  if (!loans) return;
  $("#loan-list").innerHTML = loans.length
    ? table("<th>Member</th><th>Book</th><th class='num'>Copies</th><th>Issued</th><th>Returned</th><th></th>",
        loans.map((b) => `<tr><td>${esc(b.userName)}</td><td>${esc(b.bookTitle)}</td><td class="num">${b.quantity}</td><td>${when(b.borrowedAt)}</td>
          <td>${b.returnedAt ? when(b.returnedAt) : '<span class="stamp out">Out</span>'}</td>
          <td class="actions">${b.returnedAt ? "" : `<button data-return="${b.id}">Mark returned</button>`}</td></tr>`).join(""))
    : `<p class="empty">${loanStatus === "active" ? "No books are out right now." : "No borrowing history yet."}</p>`;
}
$("#issue-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (await attempt(() => Api.post("/api/borrowings", Object.fromEntries(new FormData(e.target))))) {
    toast("Book issued.");
    loadCirculation();
  }
});
$("#loan-filter").addEventListener("click", (e) => {
  if (!e.target.dataset.status) return;
  loanStatus = e.target.dataset.status;
  document.querySelectorAll("#loan-filter button").forEach((b) => b.classList.toggle("active", b === e.target));
  loadLoans();
});
$("#loan-list").addEventListener("click", async (e) => {
  const id = e.target.dataset.return;
  if (id && (await attempt(() => Api.post("/api/borrowings/" + id + "/return")))) {
    toast("Book returned.");
    loadCirculation();
  }
});

start();
