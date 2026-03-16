import React, { useEffect } from "react";
import { Link } from "react-router-dom";

export default function PrivacyPolicy() {
  useEffect(() => {
    document.title = "Privacy Policy | Legal Aid";
    window.scrollTo(0, 0);
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 px-6 py-10">
      <div className="max-w-4xl mx-auto bg-white rounded-xl shadow-lg p-8 md:p-12">
        <h1 className="text-3xl md:text-4xl font-bold text-blue-950 mb-2">
          Privacy Policy
        </h1>
        <p className="text-sm text-gray-500 mb-8">Last Updated: January 2026</p>

        <div className="space-y-8 text-gray-700 leading-relaxed">
          <section>
            <p className="mb-4">
              This Privacy Policy describes how the Legal Aid Matching Platform ("we", "our", "us") collects, uses, stores, and protects your personal information when you use our web and mobile application.
            </p>
            <p>
              Our platform connects citizens with verified lawyers and non-governmental organizations (NGOs) for legal assistance.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">1. Information We Collect</h2>
            <p className="mb-2">We may collect the following categories of information:</p>

            <div className="ml-4 mb-3">
              <h3 className="font-medium text-gray-800">a) Personal Information</h3>
              <ul className="list-disc pl-5 mt-1 space-y-1">
                <li>Full name</li>
                <li>Email address</li>
                <li>Phone number</li>
                <li>Address (if required for legal services)</li>
              </ul>
            </div>

            <div className="ml-4 mb-3">
              <h3 className="font-medium text-gray-800">b) Identity Information (only when necessary and with consent)</h3>
              <ul className="list-disc pl-5 mt-1 space-y-1">
                <li>Aadhaar number</li>
                <li>PAN number</li>
                <li>Other government-issued identity documents required for legal verification</li>
              </ul>
            </div>

            <div className="ml-4 mb-3">
              <h3 className="font-medium text-gray-800">c) Case and Service Information</h3>
              <ul className="list-disc pl-5 mt-1 space-y-1">
                <li>Legal issue descriptions</li>
                <li>Uploaded documents</li>
                <li>Communication history with lawyers or NGOs</li>
              </ul>
            </div>

            <div className="ml-4 mb-3">
              <h3 className="font-medium text-gray-800">d) Technical Information</h3>
              <ul className="list-disc pl-5 mt-1 space-y-1">
                <li>IP address</li>
                <li>Device type</li>
                <li>Browser type</li>
                <li>Operating system</li>
                <li>Date and time of visits</li>
                <li>Pages accessed within the application</li>
              </ul>
            </div>

            <p className="mt-2">
              We do not attempt to link technical identifiers such as IP addresses to individual identities unless required to investigate misuse, fraud, or security threats.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">2. Purpose of Data Collection</h2>
            <p>We use collected data to:</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Match users with suitable lawyers or NGOs</li>
              <li>Enable communication between users and service providers</li>
              <li>Schedule consultations and case follow-ups</li>
              <li>Improve platform features and performance</li>
              <li>Maintain system security</li>
              <li>Comply with applicable legal obligations</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">3. Data Sharing and Disclosure</h2>
            <p>We do not sell, rent, or trade your personal information to any third party.</p>
            <p className="mt-2">Your information may be shared only with:</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Lawyers or NGOs assigned to your case</li>
              <li>Platform administrators for support and verification</li>
              <li>Law enforcement or government authorities when legally required</li>
            </ul>
            <p className="mt-2">
              All third parties are required to maintain confidentiality and data security.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">4. Data Security Measures</h2>
            <p>
              We implement adequate technical and organizational measures to protect your data, including:
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Secure server infrastructure</li>
              <li>Encrypted data transmission</li>
              <li>Restricted access to sensitive data</li>
              <li>Authentication and authorization controls</li>
              <li>Regular monitoring and security updates</li>
            </ul>
            <p className="mt-2">
              While we strive to protect your information, no system can guarantee complete security over the internet.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">5. Cookies and Analytics</h2>
            <p>We may use cookies and similar technologies to:</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Maintain login sessions</li>
              <li>Analyze usage patterns</li>
              <li>Improve user experience</li>
            </ul>
            <p className="mt-2">
              Users can manage or disable cookies through browser settings; however, some platform features may not function properly.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">6. User Rights</h2>
            <p>Users have the right to:</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Access their personal data</li>
              <li>Request correction of inaccurate information</li>
              <li>Request deletion of data (subject to legal requirements)</li>
              <li>Withdraw consent for optional data processing</li>
            </ul>
            <p className="mt-2">
              Requests can be made through the platform support section.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">7. Data Retention</h2>
            <p>We retain personal data only as long as necessary to:</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Provide requested legal services</li>
              <li>Meet regulatory and legal obligations</li>
              <li>Resolve disputes and enforce agreements</li>
            </ul>
            <p className="mt-2">
              After this period, data is securely deleted or anonymized.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">8. Children's Privacy</h2>
            <p>
              This platform is not intended for use by individuals under 18 years of age without parental or legal guardian consent.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">9. Third-Party Links</h2>
            <p>
              Our application may contain links to external websites or services. We are not responsible for the privacy practices of such third-party platforms.
            </p>
            <p className="mt-2">
              Users are advised to review the privacy policies of any external services they access.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">10. Changes to This Privacy Policy</h2>
            <p>
              We may update this Privacy Policy from time to time. Any changes will be posted on this page with an updated revision date.
            </p>
            <p className="mt-2">
              Continued use of the platform after changes indicates acceptance of the revised policy.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-blue-900 mb-3">11. Contact Information</h2>
            <p>
              For questions or concerns regarding this Privacy Policy or your data, contact:
            </p>
            <div className="mt-3 bg-gray-50 p-4 rounded-lg border border-gray-200">
              <p className="font-medium">Legal Aid Matching Platform Support Team</p>
              <p className="text-blue-600">Email: support@legalaidplatform.example</p>
            </div>
          </section>

          <hr className="my-8 border-gray-200" />

          <p className="text-sm text-gray-500 italic text-center">
            This privacy policy is provided for project and academic purposes and should be reviewed by legal professionals before deploying in a production environment.
          </p>
        </div>
      </div>
    </div>
  );
}
