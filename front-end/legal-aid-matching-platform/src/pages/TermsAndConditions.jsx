import { useEffect } from "react";

export default function TermsAndConditions() {
  useEffect(() => {
    document.title = "Terms & Conditions | Legal Aid";
    window.scrollTo(0, 0);
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 px-6 py-10">
      <div className="max-w-4xl mx-auto bg-white rounded-xl shadow-lg p-8 md:p-12">
        <h1 className="text-3xl md:text-4xl font-bold text-blue-950 mb-2">
          Terms & Conditions
        </h1>
        <p className="text-sm text-gray-500 mb-8">Last Updated: January 6, 2026</p>

        <div className="space-y-8 text-gray-700 leading-relaxed">
          <section>
            <p className="mb-4">
              Welcome to the Legal Aid Matching Platform . By accessing or using our website and services, you agree to be bound by these Terms and Conditions.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">1. Platform Purpose & Scope</h2>
            <p>
              The Legal Aid Matching Platform is a digital intermediary designed to connect citizens seeking legal assistance ("Citizens") with pro bono lawyers ("Lawyers") and Non-Governmental Organizations ("NGOs").
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>The Platform acts solely as a facilitator and does not provide legal advice or representation directly.</li>
              <li>We do not guarantee the outcome of any legal matter or the quality of services provided by Lawyers or NGOs.</li>
              <li>The relationship formed between a Citizen and a Lawyer/NGO is independent of the Platform.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">2. User Eligibility & Registration</h2>
            <p>
              To use our services, you must be at least 18 years old and capable of forming a binding contract.
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li><strong>Citizens:</strong> Must provide accurate personal details and case information.</li>
              <li><strong>Lawyers:</strong> Must be licensed to practice law in their respective jurisdiction and provide valid Bar Council registration details for verification.</li>
              <li><strong>NGOs:</strong> Must be validly registered organizations with a focus on legal aid or social justice.</li>
            </ul>
            <p className="mt-2">
              You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">3. Verification & Professional Conduct</h2>
            <p>
              We strive to verify the credentials of Lawyers and NGOs listed on our Platform. However:
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>We do not endorse or recommend any specific Lawyer or NGO.</li>
              <li>Lawyers and NGOs are expected to adhere to their professional codes of conduct and ethical standards.</li>
              <li>Any misconduct, negligence, or unprofessional behavior should be reported to the relevant regulatory body and the Platform administrators.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">4. Privacy & Data Protection</h2>
            <p>
              We value your privacy. Your personal information and case details are handled in accordance with our Privacy Policy.
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Sensitive case information is shared only with the Lawyer or NGO you choose to connect with.</li>
              <li>We implement reasonable security measures to protect your data, but we cannot guarantee absolute security against unauthorized access.</li>
              <li>By using the Platform, you consent to the collection and processing of your data as described in our Privacy Policy.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">5. Prohibited Activities</h2>
            <p>Users agree NOT to:</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Submit false, misleading, or fraudulent information.</li>
              <li>Use the Platform for any illegal purpose or to solicit illegal acts.</li>
              <li>Harass, abuse, or harm another person via the Platform.</li>
              <li>Attempt to gain unauthorized access to the Platform's systems or other users' accounts.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">6. Limitation of Liability</h2>
            <p>
              To the fullest extent permitted by law, the Legal Aid Matching Platform and its administrators shall not be liable for any direct, indirect, incidental, special, or consequential damages resulting from:
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>The use or inability to use the Platform.</li>
              <li>Any legal advice or services obtained through the Platform.</li>
              <li>Unauthorized access to or alteration of your transmissions or data.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">7. Modifications to Terms</h2>
            <p>
              We reserve the right to modify these Terms at any time. Changes will be effective immediately upon posting on the Platform. Your continued use of the Platform after changes constitutes your acceptance of the new Terms.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">8. Contact Us</h2>
            <p>
              If you have any questions about these Terms, please contact us at <a href="mailto:support@legalaidplatform.com" className="text-blue-600 hover:underline">support@legalaidplatform.com</a>.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
