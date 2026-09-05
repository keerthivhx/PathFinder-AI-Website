import React from 'react';
import { Download, Smartphone, ShieldCheck, CheckCircle2, QrCode, Cpu, Radio, Sparkles, ExternalLink } from 'lucide-react';

export default function DownloadSection() {
  return (
    <section id="download" className="py-24 bg-gradient-to-b from-slate-900/60 to-slate-950 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-950/70 border border-cyan-500/30 text-cyan-400 text-xs font-bold uppercase tracking-wider mb-4">
            <Smartphone className="w-3.5 h-3.5" />
            <span>Android Releases</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
            Download the PathFinder AI Android App
          </h2>
          <p className="mt-4 text-slate-400 text-base sm:text-lg">
            Experience sub-meter indoor navigation directly on your handheld Android device. Fully tested, compiled, and ready to deploy.
          </p>
        </div>

        {/* Download Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-4xl mx-auto mb-16">
          {/* Card 1: Google Play Release Bundle (.aab) */}
          <div className="relative p-8 rounded-2xl bg-gradient-to-br from-slate-900 via-slate-900 to-indigo-950/40 border border-indigo-500/30 shadow-2xl flex flex-col justify-between hover:border-indigo-400/50 transition-all">
            <div>
              <div className="flex items-center justify-between mb-6">
                <span className="text-xs font-bold px-3 py-1 rounded-full bg-indigo-950 text-indigo-400 border border-indigo-500/30">
                  Google Play Production
                </span>
                <span className="text-xs font-mono text-slate-400">v1.0 (Build 1)</span>
              </div>

              <h3 className="text-2xl font-bold text-white mb-2">Android App Bundle (.aab)</h3>
              <p className="text-sm text-slate-300 leading-relaxed mb-6">
                Signed, verified, and production-optimized release bundle for publishing to the Google Play Store or private MDM enterprise distribution.
              </p>

              <div className="space-y-2.5 mb-8">
                <div className="flex items-center gap-2.5 text-xs text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Targets modern Android 16 (API Level 36)</span>
                </div>
                <div className="flex items-center gap-2.5 text-xs text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Signed with secure production upload keystore</span>
                </div>
                <div className="flex items-center gap-2.5 text-xs text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Google Play split-APK delivery optimized</span>
                </div>
              </div>
            </div>

            <a
              href="/app-release.aab"
              download="PathFinder-AI-release.aab"
              className="w-full inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl bg-gradient-to-r from-indigo-500 to-cyan-500 text-white font-bold text-sm hover:opacity-90 shadow-lg shadow-indigo-500/20 active:scale-98 transition-all"
            >
              <Download className="w-4 h-4" />
              <span>Download Signed Bundle (.aab)</span>
            </a>
          </div>

          {/* Card 2: Direct Install APK (.apk) */}
          <div className="relative p-8 rounded-2xl bg-gradient-to-br from-slate-900 via-slate-900 to-cyan-950/40 border border-cyan-500/30 shadow-2xl flex flex-col justify-between hover:border-cyan-400/50 transition-all">
            <div>
              <div className="flex items-center justify-between mb-6">
                <span className="text-xs font-bold px-3 py-1 rounded-full bg-cyan-950 text-cyan-400 border border-cyan-500/30">
                  Direct Phone Install
                </span>
                <span className="text-xs font-mono text-slate-400">Universal APK</span>
              </div>

              <h3 className="text-2xl font-bold text-white mb-2">Android Package (.apk)</h3>
              <p className="text-sm text-slate-300 leading-relaxed mb-6">
                Install directly on any Android smartphone, tablet, or handheld hardware for rapid on-site testing, field evaluation, and demos.
              </p>

              <div className="space-y-2.5 mb-8">
                <div className="flex items-center gap-2.5 text-xs text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Compatible with Android 8.0+ through Android 16</span>
                </div>
                <div className="flex items-center gap-2.5 text-xs text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Includes offline graph topological caches</span>
                </div>
                <div className="flex items-center gap-2.5 text-xs text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Full voice narration and AR camera HUD</span>
                </div>
              </div>
            </div>

            <a
              href="/app-debug.apk"
              download="PathFinder-AI.apk"
              className="w-full inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl bg-gradient-to-r from-cyan-500 to-teal-400 text-slate-950 font-bold text-sm hover:opacity-90 shadow-lg shadow-cyan-500/20 active:scale-98 transition-all"
            >
              <Download className="w-4 h-4" />
              <span>Download Direct APK (.apk)</span>
            </a>
          </div>
        </div>

        {/* Device Requirements & Privacy Highlights */}
        <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 max-w-4xl mx-auto">
          <h4 className="text-base font-bold text-white mb-4 flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-emerald-400" />
            <span>Hardware & Privacy Compliant</span>
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs text-slate-400">
            <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800/80">
              <strong className="text-slate-200 block mb-1">Local Edge Computation</strong>
              All route graphs and step computations execute locally on device without tracking user locations.
            </div>
            <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800/80">
              <strong className="text-slate-200 block mb-1">Zero Hardware Lock-In</strong>
              Navigates via QR codes, inertial sensors, and visual landmarks—expensive BLE beacon hardware is strictly optional.
            </div>
            <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800/80">
              <strong className="text-slate-200 block mb-1">Play Policy Certified</strong>
              Zero invasive storage permissions, HTTPS-only network security, and encrypted Room local storage.
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
