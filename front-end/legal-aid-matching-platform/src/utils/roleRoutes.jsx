export function getEditPageForRole(role) {
  switch (role) {
    case "ADMIN":
      return "/dashboard/admin/profilemanagement";
    case "LAWYER":
      return "/dashboard/lawyer/editprofile";
    case "NGO":
      return "/dashboard/ngo/editprofile";
    case "CITIZEN":
      return "/dashboard/citizen/editprofile";
    default:
      return "/unauthorized"; // fallback
  }
}
