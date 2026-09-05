const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const connectDB = require('./config/db');
const apiRoutes = require('./routes');
const errorHandler = require('./middleware/errorHandler');
const { errorResponse } = require('./utils/responseFormatter');
const { seedDatabase } = require('./utils/seedData');

// Load environment variables from .env
dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

// Body parser
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// CORS configuration
const corsOrigin = process.env.CORS_ORIGIN || '*';
app.use(cors({
  origin: corsOrigin,
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

// Request logger for development
if (process.env.NODE_ENV !== 'production') {
  app.use((req, res, next) => {
    console.log(`[HTTP ${req.method}] ${req.url}`);
    next();
  });
}

// Mount API routes
app.use('/api', apiRoutes);

// Root greeting
app.get('/', (req, res) => {
  res.json({
    name: 'PathFinder AI - Indoor Navigation REST API',
    status: 'ACTIVE',
    documentation: '/api/health',
    version: '1.0.0'
  });
});

// 404 Catch-All Handler
app.use((req, res) => {
  return errorResponse(res, `Route ${req.originalUrl} not found`, 404);
});

// Centralized Error Handler Middleware
app.use(errorHandler);

// Start server
const startServer = async () => {
  // Attempt MongoDB connection
  const conn = await connectDB();
  if (conn) {
    // Optionally seed database on startup
    await seedDatabase();
  }

  const server = app.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`🚀 PathFinder AI Backend Server listening on port ${PORT}`);
    console.log(`📍 Environment: ${process.env.NODE_ENV || 'development'}`);
    console.log(`📡 Health Check: http://localhost:${PORT}/api/health`);
    console.log(`====================================================`);
  });

  return { app, server };
};

if (require.main === module) {
  startServer();
}

module.exports = { app, startServer };
