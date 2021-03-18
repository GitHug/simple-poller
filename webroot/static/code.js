const tableBody = document.querySelector('#service-table-body');
let servicesRequest = new Request('/services');
fetch(servicesRequest)
    .then(function(response) { return response.json(); })
    .then(function(serviceList) {
        serviceList.forEach(service => {
            const tr = document.createElement("tr");

            const url = document.createElement("th");
            url.appendChild(document.createTextNode(service.url));

            const name = document.createElement("th");
            name.appendChild(document.createTextNode(service.name));

            const updatedAt = document.createElement("th");
            updatedAt.appendChild(document.createTextNode(service.updated_at));

            const status = document.createElement("th");
            status.appendChild(document.createTextNode(service.status));

            tr.appendChild(url);
            tr.appendChild(name);
            tr.appendChild(updatedAt);
            tr.appendChild(status);

            tableBody.appendChild(tr);
        });
    });

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let urlName = document.querySelector('#url-name').value;
    fetch('/services', {
        method: 'post',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({url:urlName})
    }).then(res=> location.reload());
}
