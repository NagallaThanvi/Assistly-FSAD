import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';

// Placeholder Components
const Home = () => (
  <div className="container mt-5">
    <h1>Welcome to Assistly</h1>
    <p>Role-Based Community Service Platform</p>
    <Link to="/login" className="btn btn-primary me-2">Login</Link>
    <Link to="/signup" className="btn btn-secondary">Sign Up</Link>
  </div>
);

const Login = () => <div className="container mt-5"><h2>Login</h2><p>JWT Auth form goes here.</p></div>;
const Signup = () => <div className="container mt-5"><h2>Signup</h2><p>Registration form goes here.</p></div>;
const UserDashboard = () => <div className="container mt-5"><h2>User Dashboard</h2><p>View Requests, Map, Communities.</p></div>;
const AdminDashboard = () => <div className="container mt-5"><h2>Admin Dashboard</h2><p>Manage Users and Communities.</p></div>;

function App() {
  return (
    <Router>
      <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
        <div className="container">
          <Link className="navbar-brand" to="/">Assistly</Link>
          <div className="navbar-nav ms-auto">
            <Link className="nav-link" to="/login">Login</Link>
            <Link className="nav-link" to="/signup">Signup</Link>
          </div>
        </div>
      </nav>
      
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/dashboard/user" element={<UserDashboard />} />
        <Route path="/dashboard/admin" element={<AdminDashboard />} />
      </Routes>
    </Router>
  );
}

export default App;
