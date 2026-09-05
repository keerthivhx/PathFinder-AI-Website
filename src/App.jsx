import React from 'react';
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import Features from './components/Features';
import InteractiveMapPreview from './components/InteractiveMapPreview';
import HowItWorks from './components/HowItWorks';
import BuildingShowcase from './components/BuildingShowcase';
import DownloadSection from './components/DownloadSection';
import TechSpecs from './components/TechSpecs';
import Footer from './components/Footer';

export default function App() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-cyan-500 selection:text-slate-950">
      <Navbar />
      <main className="flex-1">
        <Hero />
        <Features />
        <InteractiveMapPreview />
        <HowItWorks />
        <BuildingShowcase />
        <DownloadSection />
        <TechSpecs />
      </main>
      <Footer />
    </div>
  );
}
