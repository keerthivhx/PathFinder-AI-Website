const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Please provide a name'],
      trim: true,
      maxlength: 100
    },
    email: {
      type: String,
      required: [true, 'Please provide an email address'],
      unique: true,
      lowercase: true,
      trim: true,
      match: [
        /^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/,
        'Please provide a valid email address'
      ]
    },
    password: {
      type: String,
      required: [true, 'Please provide a password'],
      minlength: 6,
      select: false
    },
    role: {
      type: String,
      enum: ['USER', 'ADMIN'],
      default: 'USER'
    },
    preferences: {
      isWheelchairMode: {
        type: Boolean,
        default: false
      },
      voiceGuidanceEnabled: {
        type: Boolean,
        default: true
      },
      defaultBuildingId: {
        type: String,
        default: 'univ'
      }
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.models.User || mongoose.model('User', UserSchema);
