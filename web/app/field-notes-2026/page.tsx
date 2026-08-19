import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "RakshaNet Field Notes — Technical Documentation",
  description: "Technical documentation for RakshaNet's offline-first Android mesh, delivery semantics, recovery engineering and verified test evidence.",
  robots: { index: false, follow: false },
};

const verified = [
  ["Two-way nearby messaging", "Redmi Note 10 Pro ↔ Samsung Galaxy J8", "PASS"],
  ["Three-phone admission and targeting", "Redmi Note 10 Pro, Galaxy J8 and Galaxy A17", "PASS"],
  ["Community broadcast", "Delivered to all admitted peers in the three-phone test", "PASS"],
  ["Private delivery receipts", "Recipient-only display with a signed acknowledgement", "PASS"],
  ["Trusted reconnect and leave/rejoin", "Previously verified peers reconnect without repeated confirmation", "PASS"],
  ["Screen-off continuity", "Multi-minute screen-off followed by immediate private delivery", "PASS"],
  ["True A → B → C out-of-range bridge", "A and C outside each other’s direct range, B as sole relay", "NOT YET MEASURED"],
];

const challenges = [
  {
    title: "One-way delivery and duplicate connection races",
    problem: "Nearby can expose more than one transport alias for the same nearby device. Simultaneous connection requests produced status 8003 (already connected) and, in an earlier build, a misleading one-way UI state.",
    response: "We made a stable cryptographic fingerprint—not a Bluetooth endpoint—the peer identity. One elected side requests the connection, duplicate aliases are suppressed, and discovery loss is kept separate from an actual session loss.",
  },
  {
    title: "A healthy peer should not be reset while a new phone joins",
    problem: "A global retry loop made the original pair unstable while a third phone was being admitted.",
    response: "Recovery became bounded and per-neighbour. Existing sessions stay alive while only the affected neighbour is retried; connected discovery is duty-cycled to reduce radio contention.",
  },
  {
    title: "Screen-off is part of the real use case",
    problem: "An Activity-owned connection is fragile when Android backgrounds the UI or turns the display off.",
    response: "The foreground service owns a process-level MeshRuntime. The UI observes it; it does not own the radio session. Real devices retained usable connectivity after several minutes of screen-off time.",
  },
  {
    title: "Android 15 permissions were not equivalent to Nearby Devices",
    problem: "On a Galaxy A17, Google Play services rejected startup with 8032 (Wi-Fi state), then 8034 (coarse Location), then 8036 (fine Location), even after Nearby Devices was granted.",
    response: "The manifest no longer hides Wi-Fi and Location declarations behind an Android 12 cap. First-run setup requests the permissions the observed Nearby implementation enforces and reports permanent setup problems instead of endlessly retrying.",
  },
  {
    title: "Offline phones do not share a trustworthy clock",
    problem: "Ordering messages by device time can place a newly sent message above an earlier message received from a phone with a different clock.",
    response: "The UI uses durable local receive/display order. Packet time is retained as metadata, not treated as a globally authoritative ordering source.",
  },
];

export default function FieldNotesPage() {
  return (
    <main className="notesPage">
      <header className="notesHeader">
        <a className="notesBrand" href="/" aria-label="RakshaNet home"><span>R</span> RakshaNet</a>
        <p>Technical field notes · SmartAIthon 2026</p>
      </header>

      <article className="notesArticle">
        <div className="notesEyebrow">UNLISTED TECHNICAL DOCUMENTATION</div>
        <h1>How RakshaNet keeps a community conversation alive when the network does not.</h1>
        <p className="notesLead">RakshaNet is an offline-first Android preparedness and emergency communication prototype. Its strongest, physically tested component is a signed nearby mesh: phones discover each other locally, exchange messages without mobile data, and preserve a usable session through normal screen-off and reconnect conditions.</p>
        <div className="notesMeta"><span>Team Endeavour</span><span>Acropolis Institute of Technology &amp; Research</span><span>Updated 20 August 2026</span></div>

        <aside className="notesCallout"><strong>What this page is for.</strong> The submission deck explains the idea. These notes explain the implementation choices, the failures we saw on real phones, the evidence we have, and the limits we will not pretend are solved.</aside>

        <section>
          <h2>1. The design principle: the mesh is the transport, not the product’s only intelligence</h2>
          <p>A prediction model, an authority dashboard, or a flood-warning feed can be useful only while at least one gateway has internet. RakshaNet is designed so that those systems are optional inputs to an offline communication layer rather than a prerequisite for it. If one phone later receives a trusted update, the update can be packaged as a signed guidance packet and carried locally through nearby phones.</p>
          <div className="notesFlow" aria-label="RakshaNet architecture flow">
            <div><b>Preparedness</b><span>Bundled lessons and local progress</span></div><i>→</i>
            <div><b>Signed packet layer</b><span>Chat · SOS · guidance</span></div><i>→</i>
            <div><b>Nearby mesh</b><span>Local relaying without internet</span></div><i>→</i>
            <div><b>Optional gateway</b><span>Any approved online source</span></div>
          </div>
          <p>This means the flood-risk service is intentionally replaceable. Its current contract is a small adapter: an online source may return a risk tier, then a gateway selects a calm, human-authored template and emits a signed <code>GUIDANCE_BROADCAST</code>. It is not a live LLM in the emergency path, and the current website simulator is explicitly a preview—not a deployed authority control plane.</p>
        </section>

        <section>
          <h2>2. What is actually running on the phone</h2>
          <pre className="notesCode">Compose UI
  │
Foreground service → process-level MeshRuntime
  ├── Room: messages, peers, deduplication and delivery state
  ├── Android Keystore: device signing identity
  ├── MeshCoordinator: validation, storage and TTL relay
  └── SelectablePacketRouter
        ├── NearbyPacketRouter (physical transport)
        └── MockPacketRouter (development only)</pre>
          <p>The transport is Google Nearby Connections in <code>P2P_CLUSTER</code> mode. That is broader than “BLE only”: Nearby may choose Bluetooth Low Energy, Bluetooth Classic, Wi-Fi Direct, or another local transport available on the phones. The app never assumes an internet connection or a central server for messaging.</p>
          <p><code>PacketRouter</code> is a deliberate seam in the architecture. Screens, Room storage and routing rules do not call Nearby directly; they use the router boundary. This keeps the proven message and safety rules reusable if a future deployment needs a different local transport.</p>
        </section>

        <section>
          <h2>3. Message integrity, routing, and honest delivery language</h2>
          <div className="notesGrid">
            <div><h3>Identity and signing</h3><p>Each device has an Android Keystore ECDSA P-256 identity. A stable public-key fingerprint identifies a peer; the editable display name is only a label. Immutable packet content is signed before a message can be stored, displayed or relayed.</p></div>
            <div><h3>Relay control</h3><p>Packets have an ID, an original TTL and a remaining TTL. Every relay decrements only the mutable remaining TTL. Atomic deduplication makes repeated arrivals harmless and prevents packet loops from becoming chat spam.</p></div>
            <div><h3>Private targeting</h3><p>Private packets can travel through intermediary phones but display only on the sender and intended recipient. This is routing privacy, not end-to-end encryption: a relay is prevented from displaying the chat item, but the current protocol does not claim ciphertext confidentiality.</p></div>
          </div>
          <table className="notesTable"><thead><tr><th>Mode</th><th>Receipt shown</th><th>What it truthfully means</th></tr></thead><tbody>
            <tr><td>Community</td><td>✓ mesh</td><td>The message was durably stored locally and injected into the mesh. It does not claim every group member read it.</td></tr>
            <tr><td>Private</td><td>✓ queued → ✓✓ delivered</td><td>The first state is local durable storage. The double tick arrives only after a signed acknowledgement from the intended recipient.</td></tr>
          </tbody></table>
        </section>

        <section>
          <h2>4. Recovery engineering: the problems were the project</h2>
          <p>A mesh demo that works only while two unlocked phones sit on a desk is not useful. The following were discovered during actual device work and changed the implementation:</p>
          <div className="notesChallenges">{challenges.map((item, index) => <article key={item.title}><span>{String(index + 1).padStart(2, "0")}</span><div><h3>{item.title}</h3><p><b>Observed:</b> {item.problem}</p><p><b>Engineering response:</b> {item.response}</p></div></article>)}</div>
        </section>

        <section>
          <h2>5. Physical evidence, not only emulator claims</h2>
          <p>Tests were run on a Redmi Note 10 Pro (Android 12/API 31), Samsung Galaxy J8 (Android 10/API 29) and Samsung Galaxy A17 (Android 15/API 35). The exact three-node admission, community/private targeting, trusted reconnect and screen-off behaviours below were observed by the team on hardware. The app’s JVM suite also covers packet validation, signing, routing, relay exclusion, ACK handling and recovery policy.</p>
          <table className="notesTable"><thead><tr><th>Check</th><th>Evidence</th><th>Status</th></tr></thead><tbody>{verified.map(([check, evidence, status]) => <tr key={check}><td>{check}</td><td>{evidence}</td><td><span className={status === "PASS" ? "notesPass" : "notesPending"}>{status}</span></td></tr>)}</tbody></table>
          <aside className="notesLimit"><strong>Boundary of the current claim:</strong> three phones can join and exchange targeted or community messages, but we have not yet measured a controlled A → B → C test in which A and C are deliberately out of each other’s direct range and B is the only usable bridge. We therefore describe multi-hop routing as implemented and unit-tested, not physically range-proven.</aside>
        </section>

        <section>
          <h2>6. Emergency and guidance packets</h2>
          <p>The Alerts flow models an SOS as a structured, signed packet rather than a differently coloured chat bubble. A generic SOS can leave first; category, note and optional raw coordinates can follow as a signed update referencing the original packet. Coordinates are opt-in, are never reverse-geocoded, and remain useful offline as raw latitude/longitude.</p>
          <p>The same packet mechanism supports future guidance. A real forecast, an ML service, a trusted authority console or another warning source can become an adapter that produces a reviewed <code>GUIDANCE_BROADCAST</code>. The mesh does not need to know which prediction system supplied the information; it only validates, stores and relays the signed update.</p>
        </section>

        <section>
          <h2>7. Current limits and next engineering work</h2>
          <ul className="notesList">
            <li>Range, dense-network throughput, route quality and the controlled out-of-range bridge test still need measured physical evidence.</li>
            <li>OEM battery management is variable; multi-minute screen-off continuity is observed, while the 30-minute idle gate remains open.</li>
            <li>The current protocol provides signed integrity and sender identity, not end-to-end encryption, membership credentials, or spam/rate controls.</li>
            <li>The flood-model data and live endpoint are not yet accepted as production-grade. The demo uses labelled historical-risk scenarios and an explicit disclosure rather than real-time prediction claims.</li>
            <li>Authority publishing, AI summaries and cloud-to-mesh bridging on the website are interface prototypes. They are not represented as operational emergency infrastructure.</li>
          </ul>
        </section>

        <section>
          <h2>8. Reproducibility and references</h2>
          <p>The Android verification gate is <code>gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug</code>. The public site is a Git-connected Next.js project and deploys from the repository’s <code>web/</code> directory after a push to <code>main</code>.</p>
          <ul className="notesReferences">
            <li><a href="https://developers.google.com/nearby/connections/overview" target="_blank" rel="noreferrer">Google Nearby Connections overview</a></li>
            <li><a href="https://developer.android.com/develop/background-work/services/foreground-services" target="_blank" rel="noreferrer">Android foreground services</a></li>
            <li><a href="https://developer.android.com/privacy-and-security/keystore" target="_blank" rel="noreferrer">Android Keystore</a></li>
            <li><a href="https://ndma.gov.in/Natural-Hazards/Floods" target="_blank" rel="noreferrer">National Disaster Management Authority: floods</a></li>
            <li><a href="https://github.com/makekush7-netizen/RakshaNet" target="_blank" rel="noreferrer">RakshaNet source repository</a></li>
          </ul>
        </section>

        <footer className="notesFooter">RakshaNet · Team Endeavour · Technical documentation for SmartAIthon 2026</footer>
      </article>
    </main>
  );
}
