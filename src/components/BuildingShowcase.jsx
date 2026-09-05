import React from 'react';
import { GraduationCap, Hospital, Plane, ShoppingBag, Layers, ArrowUpRight } from 'lucide-react';

const facilities = [
  {
    icon: GraduationCap,
    title: "Universities & Campuses",
    floors: "4 Multi-Level Floors",
    desc: "Engineering blocks, faculty research laboratories, smart lecture halls, auditoriums, and registrar offices.",
    highlights: ["CS & Robotics Wing", "3D Prototyping FabLab", "Multi-floor elevator corridors"],
    gradient: "from-blue-600/20 to-cyan-500/10 border-blue-500/30"
  },
  {
    icon: Hospital,
    title: "Hospitals & Medical Centers",
    floors: "3 Critical Care Floors",
    desc: "Emergency trauma ER intake, advanced diagnostic MRI suites, cardiac units, surgical pavilions, and pharmacies.",
    highlights: ["Symptom-based search", "Step-free gurney routing", "Sanitized wing paths"],
    gradient: "from-emerald-600/20 to-teal-500/10 border-emerald-500/30"
  },
  {
    icon: Plane,
    title: "Airports & Transit Terminals",
    floors: "2 Massive Terminal Levels",
    desc: "Flight check-in rows, security screening checkpoints, departure boarding gates A1-B24, baggage carousels, and VIP sky lounges.",
    highlights: ["Boarding gate timers", "Customs checkpoint transit", "Duty free navigation"],
    gradient: "from-purple-600/20 to-indigo-500/10 border-purple-500/30"
  },
  {
    icon: ShoppingBag,
    title: "Commercial Malls & Retail Hubs",
    floors: "2 Retail & Entertainment Levels",
    desc: "Multi-anchor retail departments, central food courts, IMAX cinema theatres, escalators, and accessible family restrooms.",
    highlights: ["Store locator", "Restroom & parent room finding", "Parking bay wayfinding"],
    gradient: "from-amber-600/20 to-orange-500/10 border-amber-500/30"
  }
];

export default function BuildingShowcase() {
  return (
    <section id="facilities" className="py-24 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-950/70 border border-emerald-500/30 text-emerald-400 text-xs font-bold uppercase tracking-wider mb-4">
            <Layers className="w-3.5 h-3.5" />
            <span>Pre-Configured Environments</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
            Ready-to-Explore Multi-Floor Facilities
          </h2>
          <p className="mt-4 text-slate-400 text-base sm:text-lg">
            Complete architectural graphs, waypoint networks, and semantic points of interest bundled ready to navigate.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {facilities.map((fac, idx) => {
            const Icon = fac.icon;
            return (
              <div
                key={idx}
                className={`p-7 rounded-2xl bg-gradient-to-br ${fac.gradient} border bg-slate-900/90 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl`}
              >
                <div className="flex items-center justify-between mb-4">
                  <div className="w-12 h-12 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center text-cyan-400">
                    <Icon className="w-6 h-6" />
                  </div>
                  <span className="text-xs font-mono font-bold px-3 py-1 rounded-full bg-slate-950 text-slate-300 border border-slate-800">
                    {fac.floors}
                  </span>
                </div>

                <h3 className="text-xl font-bold text-white mb-2">{fac.title}</h3>
                <p className="text-sm text-slate-400 leading-relaxed mb-6">{fac.desc}</p>

                <div className="flex flex-wrap gap-2 pt-4 border-t border-slate-800/80">
                  {fac.highlights.map((h, i) => (
                    <span key={i} className="text-xs font-medium px-2.5 py-1 rounded-md bg-slate-950/80 text-slate-300 border border-slate-800">
                      ✓ {h}
                    </span>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
