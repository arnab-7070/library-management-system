// ---------------------------------------------------------------
// This file is the ONLY place where the frontend talks to the backend.
//
// API_BASE is the address of the Java server.
//   ""                       -> the page is served by the Java server itself
//                               (http://localhost:8080), the easiest setup.
//   "http://localhost:8080"  -> use this if you open index.html some other way
//                               (VS Code Live Server, GitHub Pages, ...).
// ---------------------------------------------------------------
const API_BASE = "";

const Api = {
  token: sessionStorage.getItem("token"),

  setToken(t) {
    this.token = t;
    if (t) sessionStorage.setItem("token", t);
    else sessionStorage.removeItem("token");
  },

  // sends one HTTP request and returns the JSON the server answers with
  async request(method, path, body) {
    const headers = { "Content-Type": "application/json" };
    if (this.token) headers["Authorization"] = "Bearer " + this.token;

    let res;
    try {
      res = await fetch(API_BASE + path, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
      });
    } catch (e) {
      throw new Error("Cannot reach the server. Check that the Java app is running.");
    }
    const data = await res.json().catch(() => ({}));
    if (res.status === 401 && this.token) {
      this.setToken(null);
      location.reload(); // session expired: go back to the sign-in screen
    }
    if (!res.ok) throw new Error(data.error || "Request failed (" + res.status + ")");
    return data;
  },

  get: (path) => Api.request("GET", path),
  post: (path, body) => Api.request("POST", path, body || {}),
  del: (path) => Api.request("DELETE", path),
};
