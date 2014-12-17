All data files are temporary stored here:
  User input (Uploaded or retrieved from a repository)
  Intermediary files
  Ouput files 

Files are deleted when there are no jobs outstanding that need those files and a user fetches his or her data.
Call the tools/cleanup servlet to delete files that have not been fetched by the user and that no job is going to need.

Example (Cron)

0 5 * * * curl https://localhost:8080/tools/cleanup