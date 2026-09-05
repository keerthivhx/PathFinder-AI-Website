const { errorResponse } = require('../utils/responseFormatter');

/**
 * Global Centralized Error Handling Middleware
 */
const errorHandler = (err, req, res, next) => {
  console.error(`[API Error] ${req.method} ${req.originalUrl}:`, err);

  // Mongoose validation error
  if (err.name === 'ValidationError') {
    const messages = Object.values(err.errors).map(val => val.message);
    return errorResponse(res, 'Validation Error', 400, messages);
  }

  // Mongoose duplicate key
  if (err.code === 11000) {
    return errorResponse(res, 'Duplicate field value entered', 400);
  }

  // Mongoose CastError (bad ObjectId)
  if (err.name === 'CastError') {
    return errorResponse(res, `Resource not found with id of ${err.value}`, 404);
  }

  // JWT errors
  if (err.name === 'JsonWebTokenError') {
    return errorResponse(res, 'Invalid authorization token', 401);
  }
  if (err.name === 'TokenExpiredError') {
    return errorResponse(res, 'Authorization token has expired', 401);
  }

  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal Server Error';

  return errorResponse(res, message, statusCode);
};

module.exports = errorHandler;
