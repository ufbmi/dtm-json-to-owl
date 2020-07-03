sed -n ' /lookup value is.*/ {N; /null individual or OP for OP assertion/ P;}' papers-json.err | sort | uniq -c | sort | > missing-journal-connections-with-counts.txt

