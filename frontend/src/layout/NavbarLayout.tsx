import { NavLink, Outlet } from 'react-router';

const NavbarLayout = () => {
  return (
    <>
      <nav className="navbar">
        <NavLink to="/" className="navbar-logo">CineSearch</NavLink>
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
        </div>
      </nav>
      <Outlet />
    </>
  );
};

export default NavbarLayout;
