import { NavLink, Outlet } from 'react-router';

const NavbarLayout = () => {
  return (
    <>
      <nav className="navbar">
        <NavLink to="/" className="navbar-logo">
          <span className="navbar-logo-mark">C</span>
          <span className="navbar-logo-text">Cine<span>Search</span></span>
        </NavLink>
        <div className="navbar-links">
          <NavLink
            to="/"
            end
            className={({ isActive }) => `navbar-link${isActive ? ' active' : ''}`}
          >
            Home
          </NavLink>
          <NavLink
            to="/movies"
            className={({ isActive }) => `navbar-link${isActive ? ' active' : ''}`}
          >
            Movies
          </NavLink>
          <NavLink
            to="/swipe"
            className={({ isActive }) => `navbar-link${isActive ? ' active' : ''}`}
          >
            Swipe
          </NavLink>
        </div>
      </nav>
      <Outlet />
    </>
  );
};

export default NavbarLayout;
