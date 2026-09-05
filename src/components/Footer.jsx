import React from 'react';
import { Navigation, Heart, Shield, Github } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="border-t border-slate-800 bg-slate-950 py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          {/* Brand */}
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center text-slate-950 font-black">
              <Navigation className="w-4 h-4 fill-current" />
            </div>
            <div>
              <span className="font-extrabold text-white text-base tracking-tight">PathFinder AI</span>
              <span className="text-xs text-slate-500 ml-2">© {new Date().getFullYear()} All rights reserved.</span>
            </div>
          </div>

          {/* Links */}
          <div className="flex flex-wrap items-center gap-6 text-xs text-slate-400">
            <a href="#features" className="hover:text-cyan-400 transition-colors">Features</a>
            <a href="#interactive-demo" className="hover:text-cyan-400 transition-colors">Interactive Map</a>
            <a href="#how-it-works" className="hover:text-cyan-400 transition-colors">Workflow</a>
            <a href="#facilities" className="hover:text-cyan-400 transition-colors">Facilities</a>
            <a href="#download" className="hover:text-cyan-400 transition-colors">Downloads</a>
          </div>

          {/* Status badge */}
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-900 border border-slate-800 text-xs text-slate-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
            <span>Vercel Production Active</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
