# Architecture Notes

## Recommended starting architecture

Start with a modular monolith rather than separate microservices. This keeps development and deployment manageable while preserving clear boundaries between identity, organizations, membership, notices, notifications, and platform administration.

## Tenant isolation

Every organization-owned record must carry an organization identifier. Every backend read and write must verify the authenticated user's membership and permission within the active organization. Never trust a tenant identifier supplied by the browser without checking membership on the server.

Tenant scope must also apply to search, counts, pagination, exports, uploaded files, and background jobs—not only individual notice detail pages.

## Authorization model

Begin with a small fixed role set:
- owner
- admin
- publisher
- member

Keep platform administrator permissions separate from organization roles. Assign organization roles per membership, because the same person may have different roles in different organizations.

## Core domain entities

- User
- Organization
- Membership (user + organization + role + status)
- Notice (organization + author + category + content + status + timestamps)
- Category (organization + name + display order)
- Invitation (organization + inviter + invite target + expiry + status)
- Attachment (organization + notice + storage reference)
- AuditEvent (organization + actor + action + target + timestamp)

## Security requirements

- Enforce authorization on the server for every protected operation.
- Default to private organization boards.
- Validate and sanitize user-provided notice content.
- Restrict attachment types and sizes; store files privately and authorize downloads.
- Keep credentials and API keys out of Git.
- Use environment variables for secrets.
- Record important administrative and publishing actions.
- Test that users from Organization A cannot read or modify Organization B data.

## Technology decision

Do not lock a framework or database until the existing repository and any reusable project assets have been inspected. Keep the application stack decision explicit in a later architecture decision record.
