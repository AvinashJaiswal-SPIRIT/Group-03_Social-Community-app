import { useState } from "react";
import { useNavigate } from "react-router-dom";
import logo from "./assets/nexora-logo.png";
import { API_URL } from "./api";
import "./styles.css";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");

  function checkEmail() {
    // something@something.something
    const pattern = /^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i;

    if (email === "") {
      return "Email is required";
    }
    if (!pattern.test(email)) {
      return "Enter a valid email like name@example.com";
    }
    const beforeAt = email.split("@")[0];
    if (!/[a-z]/i.test(beforeAt)) {
      return "Email name must contain at least one letter, not only numbers";
  }
    return "";
  }

  function checkPassword() {
    if (password === "") {
      return "Password is required";
    }
    if (password.length < 8) {
      return "Password must be at least 8 characters";
    }
    if (!/[a-z]/.test(password)) {
      return "Password must have a lowercase letter";
    }
    if (!/[A-Z]/.test(password)) {
      return "Password must have an uppercase letter";
    }
    if (!/[0-9]/.test(password)) {
      return "Password must have a number";
    }
    if (!/[^A-Za-z0-9]/.test(password)) {
      return "Password must have a symbol like @ # $ !";
    }
    return "";
  }

  async function handleLogin(e) {
  e.preventDefault();

  const emailMsg = checkEmail();
  const passwordMsg = checkPassword();

  setEmailError(emailMsg);
  setPasswordError(passwordMsg);

  if (emailMsg === "" && passwordMsg === "") {
    // ================= BACKEND: LOGIN (START) =================
    // try {
    //   const response = await fetch(API_URL + "/login", {
    //     method: "POST",
    //     headers: { "Content-Type": "application/json" },
    //     body: JSON.stringify({ email: email, password: password }),
    //   });

    //   let data = {};
    //   try {
    //     data = await response.json();
    //   } catch (err) {
    //     // backend sent an empty reply
    //   }

    //   if (!response.ok) {
    //     setPasswordError(data.message || "Wrong email or password");
    //     return;
    //   }

    //   localStorage.setItem("token", data.accessToken);
    //   if (data.refreshToken) {
    //     localStorage.setItem("refreshToken", data.refreshToken);
    //   }

    //   // TODO (backend): the login reply has no role. Ask the backend teammate
    //   // how to get it. Until then everyone is treated as a normal user.
    //   localStorage.setItem("role", data.role || "user");

    //   // the login reply has no name, so keep the name saved at sign up
    //   // (if not found, use the part of the email before the @)
    //   const oldUser = JSON.parse(localStorage.getItem("user") || "null");
    //   let name = email.split("@")[0];
    //   if (oldUser && oldUser.email === email) {
    //     name = oldUser.name;
    //   }
    //   localStorage.setItem("user", JSON.stringify({ name: name, email: email }));
    // } catch (err) {
    //   setPasswordError("Cannot connect to the server. Please try again.");
    //   return;
    // }  <--remove slash when backend comes
    // ================= BACKEND: LOGIN (END) ===================
    //this below one line remove when backend runs//
    localStorage.setItem("role", "user");
    localStorage.setItem("isLoggedIn", "true");

    if (localStorage.getItem("role") === "admin") {
      navigate("/admin");
    } else {
      navigate("/home");
    }
  }
}

  return (
    <div className="login-page">
      <form className="login-box" onSubmit={handleLogin}>
        <div className="brand">
          <img src={logo} alt="Nexora logo" className="brand-logo" />
          <h1>Nexora</h1>
        </div>
        <h2>Welcome back !</h2>
        <p className="small-text">Log in to your account to continue</p>

        <label>Email address</label>
        <input
          type="text"
          placeholder="Enter your email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <p className="error">{emailError}</p>

        <label>Password</label>
        <div className="password-row">
          <input
            type={showPassword ? "text" : "password"}
            placeholder="Enter your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <span
            className="eye"
            onClick={() => setShowPassword(!showPassword)}
          >
            {showPassword ? "🔒" : "🔓"}
          </span>
        </div>
        <p className="error">{passwordError}</p>

        <p className="forgot" onClick={() => navigate("/forgot-password")}>
          Forgot password?
        </p>

        <button type="submit" className="login-btn">
          Login
        </button>

        <button
          type="button"
          className="create-btn"
          onClick={() => navigate("/create-account")}
        >
          Create account
        </button>
      </form>
    </div>
  );
}

export default Login;
