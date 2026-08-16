"use client";

import { useState } from "react";

const scenarios = [
  { name: "Normal monsoon", rain: "1,800 mm", risk: "LOW", probability: 4 },
  { name: "Heavy rainfall", rain: "2,600 mm", risk: "MODERATE", probability: 61 },
  { name: "Extreme rainfall", rain: "3,500 mm", risk: "SEVERE", probability: 92 },
];

const reports = [
  { role: "Authority", time: "10:42", title: "School Road partially blocked", detail: "Waterlogging near the bridge. Use alternate route via Market Road." },
  { role: "Ambassador", time: "10:25", title: "Community hall open", detail: "Relief supplies, charging point and drinking water available." },
  { role: "Community", time: "10:10", title: "Elderly couple needs transport", detail: "Vehicle needed from Pallipuram area to Primary Health Centre." },
];

export default function Home() {
  const [scenario, setScenario] = useState(2);
  const [notice, setNotice] = useState("");
  const current = scenarios[scenario];

  const prototypeNotice = (message: string) => {
    setNotice(message);
    window.setTimeout(() => setNotice(""), 4200);
  };

  return (
    <main>
      {notice && <div className="toast" role="status">{notice}</div>}
      <header className="siteHeader">
        <a className="brand" href="#top" aria-label="RakshaNet home"><span className="shield">R</span><strong>RakshaNet</strong></a>
        <nav><a href="#how">How it works</a><a href="#simulator">Simulator</a><a href="#authority">For authorities</a></nav>
        <a className="smallButton" href="/RakshaNet-v0.3.1-ui-fix.apk" download>Download app</a>
      </header>

      <section className="hero" id="top">
        <div className="heroCopy">
          <div className="eyebrow">OFFLINE-FIRST COMMUNITY SAFETY</div>
          <h1>Connected when<br/>networks fail.</h1>
          <p>RakshaNet helps neighbourhoods prepare, share trusted updates and relay urgent messages across nearby Android phones—even when internet access disappears.</p>
          <div className="heroActions"><a className="primaryButton" href="/RakshaNet-v0.3.1-ui-fix.apk" download>Download Android app</a><a className="textLink" href="#simulator">Explore the demo →</a></div>
          <div className="downloadMeta"><span>Android 8+</span><span>Demo v0.3.1</span><span>Offline lessons included</span></div>
        </div>
        <div className="heroVisual">
          <img src="/images/flood-preparedness.jpg" alt="Neighbours preparing sandbags, water and a first aid kit before monsoon rain"/>
          <div className="meshFloat"><span className="statusDot"/>Nearby mesh ready<strong>2 phones connected</strong></div>
        </div>
      </section>

      <section className="proof" id="how">
        <article><span className="proofIcon">◎</span><h3>Offline nearby mesh</h3><p>Phones discover, reconnect and relay signed messages without cellular data.</p></article>
        <article><span className="proofIcon">✓</span><h3>Trusted community updates</h3><p>Clear labels separate authority guidance, trained ambassadors and community reports.</p></article>
        <article><span className="proofIcon">▤</span><h3>Preparedness learning</h3><p>Short visual lessons and challenges remain available entirely offline.</p></article>
      </section>

      <section className="section intro">
        <div><span className="sectionKicker">DEMONSTRATION CONSOLE</span><h2>See the future control flow.</h2></div>
        <p>This interface is a polished product preview for the hackathon. Live incident creation and official-channel publishing are not enabled yet. Future access will be restricted to verified authorities.</p>
      </section>

      <section className="console" id="simulator">
        <div className="consoleHeader"><div><span className="demoBadge">DEMO · SIMULATED TELEMETRY</span><h2>Flood drill simulator</h2></div><span className="previewTag">Interactive UI preview</span></div>
        <div className="simGrid">
          <aside className="scenarioPanel">
            <label>Region<select aria-label="Region"><option>Kerala Basin</option></select></label>
            <p className="stepLabel">Choose a scenario</p>
            {scenarios.map((item, index) => <button key={item.name} className={`scenario ${scenario === index ? "selected" : ""}`} onClick={() => setScenario(index)}><span>{item.name}</span><small>{item.rain}</small></button>)}
            <button className="darkButton" onClick={() => prototypeNotice("Risk evaluated locally for the UI demonstration.")}>Evaluate risk</button>
          </aside>
          <div className="riskPanel">
            <p className="stepLabel">Historical-risk assessment</p>
            <div className={`riskOrb ${current.risk.toLowerCase()}`}><strong>{current.risk}</strong><span>{current.probability}%</span></div>
            <p className="honesty">Model v4 · 118 annual Kerala observations<br/>Demo inputs are simulated—not live telemetry.</p>
            <h3>Suggested preparedness action</h3>
            <ul><li>Avoid unnecessary travel</li><li>Check safe routes and neighbours</li><li>Keep emergency kit ready</li></ul>
            <button className="darkButton" onClick={() => prototypeNotice("Incident publishing is a prototype. Verified authority access is coming soon.")}>Start labelled drill</button>
          </div>
          <div className="eventPanel">
            <p className="stepLabel">Proposed event route</p>
            <ol className="timeline"><li><b>Authority creates drill</b><span>Signed official event</span></li><li><b>Online gateway receives</b><span>Aman&apos;s Redmi</span></li><li><b>Nearby mesh relays</b><span>2 offline phones</span></li><li><b>Field updates return</b><span>When any gateway is online</span></li></ol>
            <div className="gateway"><span className="statusDot"/><div><small>ONLINE GATEWAY</small><strong>Aman&apos;s Redmi</strong></div><b>READY</b></div>
          </div>
        </div>
      </section>

      <section className="authority" id="authority">
        <div className="authorityTop"><div><span className="liveBadge">KERALA FLOOD DRILL · PREVIEW</span><h2>Authority situation room</h2></div><div className="restricted">Restricted authority access · coming soon</div></div>
        <div className="metrics"><div><strong>4</strong><span>Field updates</span></div><div><strong>2</strong><span>Help requests</span></div><div><strong>1</strong><span>Online gateway</span></div></div>
        <div className="authorityGrid">
          <div className="reportColumn"><h3>Community field reports</h3>{reports.map(report => <article className="report" key={report.title}><div><span className={`role ${report.role.toLowerCase()}`}>{report.role}</span><time>{report.time}</time></div><h4>{report.title}</h4><p>{report.detail}</p></article>)}</div>
          <div className="aiDraft"><div className="draftBadge">AI-ASSISTED DRAFT · HUMAN REVIEW REQUIRED</div><h3>Situation summary</h3><h5>Confirmed</h5><ul><li>Heavy rainfall continuing</li><li>School Road partially blocked</li><li>Community hall open</li></ul><h5>Urgent needs</h5><ul><li>Transport for an elderly couple</li><li>Sandbags for low-lying homes</li></ul><h5>Contradiction to verify</h5><p>Two reports differ on bridge status. Ask for a fresh first-hand update.</p><a href="#sources">View 4 source reports →</a></div>
          <div className="broadcast"><h3>Official broadcast</h3><label>Send to<select><option>All nearby communities</option></select></label><textarea defaultValue="Heavy rain is expected to continue. Avoid School Road and use verified safe routes. Check on neighbours and keep emergency supplies ready. This is a drill."/><div className="approved">✓ Calm, actionable, and marked as a drill</div><button className="darkButton" onClick={() => prototypeNotice("Official publishing requires verified authority access and is not enabled in this prototype.")}>Review and broadcast</button><small>Future messages will be signed by a verified authority identity.</small></div>
        </div>
      </section>

      <section className="disclosure" id="sources"><strong>What is real today?</strong><p>The Android app already supports physically tested nearby community/private messaging, trusted reconnect, screen-off relay, SOS packets and offline learning. This website&apos;s authority publishing, AI analysis and cloud-to-mesh bridge are visual prototypes planned after the hackathon deadline.</p></section>

      <footer><div className="brand"><span className="shield">R</span><strong>RakshaNet</strong></div><p>Prepare together. Stay connected when networks fail.</p><span>Hackathon demonstration · 2026</span></footer>
    </main>
  );
}
