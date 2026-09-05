const jwt = require('jsonwebtoken');
const { errorResponse } = require('../utils/responseFormatter');

/**
 * Protect routes - verify Bearer JWT token
 */
const protect = (req, res, next) => {
  let token;
  if (
    req.headers.authorization &&
    req.headers.authorization.startsWith('Bearer')
  ) {
    token = req.headers.authorization.split(' ')[1];
  }

  if (!token) {
    return errorResponse(res, 'Not authorized to access this resource. Missing token.', 401);
  }

  try {
    const secret = process.env.JWT_SECRET || 'your_jwt_super_secret_key_change_in_production';
    const decoded = jwt.verify(token, secret);
    req.user = decoded;
    next();
  } catch (err) {
    return errorResponse(res, 'Invalid or expired token', 401);
  }
};

/**
 * Authorize specific roles (e.g. 'ADMIN')
 */
const authorize = (...roles) => {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return errorResponse(
        res,
        `User role '${req.user?.role}' is not authorized to access this route`,
        403
      );
    }
    next();
  };
};

const requireAdmin = authorize('ADMIN');

module.exports = {
  protect,
  authorize,
  verifyToken: protect,
  requireAdmin
};
