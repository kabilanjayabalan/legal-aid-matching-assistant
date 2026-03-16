export default function Footer() {
    return(
        <footer className="bg-blue-950 text-white px-4 md:px-10 py-2 text-center">
        <p>© {new Date().getFullYear()} Legal-Aid Matching Platform. All rights reserved.</p>
      </footer>
    );
}