"""Fetch optional smoke-test mods and required dependencies; never bundle them."""
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.parse import urlencode

out = Path('neoforge/.smoke-mods')
out.mkdir(parents=True, exist_ok=True)
expected = 'f022b34217c7ce56dd0bac1da253ae7e7a4017f1a3595bc20b434798a22991a3761fa5341874d0f4f1030dde8a18de3af6b269a93c7b5c3b6326f7a27151fb70'
query = urlencode({'game_versions': '["1.21.1"]', 'loaders': '["neoforge"]'})

def api(path):
    return json.load(urlopen(Request('https://api.modrinth.com/v2/' + path,
        headers={'User-Agent': 'TNKTerraForged/compatibility-smoke'}), timeout=60))

# Lithostitched 1.8.1 declares Apollib in its JAR but omits it from Modrinth metadata.
queue = [('natures-spirit', None), ('lithostitched', None), ('terrablender', None), ('apollib', None)]
seen = set()
while queue:
    project, version_id = queue.pop(0)
    if version_id:
        versions = [api('version/' + version_id)]
    else:
        versions = api(f'project/{project}/version?{query}')
    if project == 'natures-spirit':
        files = [(v, f) for v in versions for f in v['files'] if f['hashes'].get('sha512') == expected]
    else:
        if project == 'terrablender':
            versions = [v for v in versions if '4.1.0.8' in v['version_number']]
        else:
            versions = [v for v in versions if v['version_type'] == 'release'] or versions
        assert versions, f'No compatible version for required mod {project}'
        files = [(versions[0], f) for f in versions[0]['files'] if f.get('primary')]
    assert files, f'Cannot locate exact compatible {project} smoke-test artifact'
    version, file = files[0]
    if version['project_id'] in seen:
        continue
    seen.add(version['project_id'])
    data = urlopen(file['url'], timeout=60).read()
    assert hashlib.sha512(data).hexdigest() == file['hashes']['sha512']
    (out / file['filename']).write_bytes(data)
    print(project, version['id'], version['version_number'], file['hashes']['sha512'])
    for dependency in version.get('dependencies', []):
        if dependency['dependency_type'] == 'required':
            assert dependency.get('project_id'), f'Unresolvable required dependency: {dependency}'
            queue.append((dependency['project_id'], dependency.get('version_id')))
