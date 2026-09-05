import React from 'react';
import { Download, Sparkles, Compass, ShieldCheck, ArrowRight, Smartphone, CheckCircle2, Zap } from 'lucide-react';

export default function Hero() {
  return (
    <section className="relative pt-32 pb-20 md:pt-40 md:pb-28 overflow-hidden">
      {/* Background Gradients */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-cyan-500/10 rounded-full blur-3xl pointer-events-none -z-10" />
      <div className="absolute top-1/3 right-10 w-[400px] h-[400px] bg-purple-500/10 rounded-full blur-3xl pointer-events-none -z-10" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col items-center text-center max-w-3xl mx-auto">
          {/* Badge */}
          <div className="inline-flex items-center gap-2.5 px-4 py-1.5 rounded-full bg-cyan-950/70 border border-cyan-500/30 text-cyan-300 text-xs font-bold tracking-wider uppercase mb-8 shadow-inner">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>PATHFINDER AI • INDOOR WAYFINDING</span>
          </div>

          {/* Heading */}
          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-black text-white tracking-tight leading-[1.1] mb-6">
            Navigate Smarter.<br />
            <span className="bg-gradient-to-r from-cyan-400 via-teal-300 to-indigo-400 bg-clip-text text-transparent">
              Find Your Way.
            </span>
          </h1>

          {/* Subheading */}
          <p className="text-lg sm:text-xl text-slate-300 font-normal leading-relaxed mb-10 max-w-2xl">
            AI-powered indoor navigation that helps you reach any destination quickly, easily, and intelligently across multi-floor campuses, hospitals, airports, and facilities.
          </p>

          {/* Hero CTAs */}
          <div className="flex flex-col sm:flex-row items-center gap-4 w-full sm:w-auto mb-14">
            <a
              href="#download"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-3 px-8 py-4 rounded-2xl bg-gradient-to-r from-cyan-500 to-teal-400 text-slate-950 font-bold text-base hover:opacity-95 shadow-xl shadow-cyan-500/25 transition-all transform hover:-translate-y-0.5 active:translate-y-0"
            >
              <Smartphone className="w-5 h-5 text-slate-950" />
              <span>Download Android App</span>
              <ArrowRight className="w-4 h-4 text-slate-950" />
            </a>

            <a
              href="#interactive-demo"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-8 py-4 rounded-2xl bg-slate-900/90 hover:bg-slate-800 border border-slate-700 text-slate-200 font-semibold text-base transition-all"
            >
              <Compass className="w-5 h-5 text-cyan-400" />
              <span>Interactive Map Demo</span>
            </a>
          </div>

          {/* Highlights bar */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6 w-full pt-8 border-t border-slate-800/80">
            <div className="flex flex-col items-center p-3 rounded-xl bg-slate-900/40 border border-slate-800/60">
              <span className="text-2xl sm:text-3xl font-black text-cyan-400">Sub-Meter</span>
              <span className="text-xs text-slate-400 font-medium mt-1">Indoor Precision</span>
            </div>
            <div className="flex flex-col items-center p-3 rounded-xl bg-slate-900/40 border border-slate-800/60">
              <span className="text-2xl sm:text-3xl font-black text-purple-400">Gemini AI</span>
              <span className="text-xs text-slate-400 font-medium mt-1">Smart Route Optimizer</span>
            </div>
            <div className="flex flex-col items-center p-3 rounded-xl bg-slate-900/40 border border-slate-800/60">
              <span className="text-2xl sm:text-3xl font-black text-emerald-400">Multi-Floor</span>
              <span className="text-xs text-slate-400 font-medium mt-1">Elevator & Ramp Paths</span>
            </div>
            <div className="flex flex-col items-center p-3 rounded-xl bg-slate-900/40 border border-slate-800/60">
              <span className="text-2xl sm:text-3xl font-black text-indigo-400">100% Offline</span>
              <span className="text-xs text-slate-400 font-medium mt-1">Embedded Graph Engine</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
