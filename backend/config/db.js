const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    const mongoUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/pathfinder_ai';
    
    // Connect with Mongoose
    const conn = await mongoose.connect(mongoUri, {
      serverSelectionTimeoutMS: 5000
    });

    console.log(`[MongoDB] Connected successfully: ${conn.connection.host}/${conn.connection.name}`);
    return conn;
  } catch (error) {
    console.warn(`[MongoDB Warning] Could not connect to MongoDB at ${process.env.MONGODB_URI || 'mongodb://localhost:27017/pathfinder_ai'}: ${error.message}`);
    console.warn(`[MongoDB Notice] The API will utilize in-memory mock repositories and fallback demo dataset when database connection is inactive.`);
    return null;
  }
};

module.exports = connectDB;
